import com.jcraft.jsch.Session
import com.jcraft.jsch.Channel
import com.jcraft.jsch.ChannelSftp

nginxConfigFile = "/usr/local/nginx/conf/nginx.conf"

writeNginxConfig = { nginx, tomcat ->

  def config = """
  user www-data;
worker_processes  4;

events {
    worker_connections  1024;
}

http {
    include       mime.types;
    default_type  application/octet-stream;

    sendfile        on;

    keepalive_timeout  10;

    gzip            on;
    gzip_comp_level 2;
    gzip_proxied    any;
    gzip_types      text/plain text/css application/x-javascript text/xml application/xml application/xml+rss text/javascript;
    tcp_nodelay     on;
    upstream backend {
       server ${tomcat.internal}:8080;

       #server   10.176.64.138:8080;
       #server   10.176.64.119:8080;
       #server   10.176.64.108:8080;
    }
    proxy_cache_path  /tmp/cache  keys_zone=tmpcache:10m;
    log_format  main  '\$host - \$remote_user [\$time_local] "\$request" '
                  '\$status \$body_bytes_sent "\$http_referer" '
                  '"\$http_user_agent" "\$http_x_forwarded_for"';

   #rate abuse conf
   limit_req_zone \$binary_remote_addr zone=zone_rate:10m rate=1r/s;

   #block unwanted traffic
   include blockips.conf;

   access_log /usr/local/nginx/logs/access.log main;"""

   if (nginx.name != "nginx6") {
    config += """
     server {
         server_name  _;  #default
         return 444;
     }
    """
   }
   config += "\ninclude /usr/local/nginx/sites-enabled/*;\n}\n"

  def tmp = File.createTempFile("nginx", "config")
  tmp << config

  currentServer = nginx

  copy(tmp.absolutePath, nginxConfigFile)

  execRemote("/usr/local/sbin/nginx -t -c $nginxConfigFile")

  execRemote("kill -HUP `cat /usr/local/nginx/logs/nginx.pid`")
}

getAllNginxTargets = {

  def ret = [:]

  nginx.each { server ->
    ssh(server) { Session session ->
      try {
        Channel channel=session.openChannel("sftp");
        channel.connect();
        ChannelSftp sftp =(ChannelSftp)channel;

        def nginxConfig = sftp.get(nginxConfigFile).text

        def backEndRegex = /[\s\w\W]*backend[\s\n\r]*(\{[\w\W]*)proxy[\s\w\W]*/

        def matcher = ( nginxConfig =~ backEndRegex)

        def backendBlock = matcher[0][1].trim()

        def configuredBackends = backendBlock.trim().substring(1, backendBlock.length() - 1).split(";").collect { it.trim() }

        def activeBackend = configuredBackends.find { !it.contains("#")}
        activeBackend = activeBackend.substring(6, activeBackend.indexOf(":")).trim()
        ret[server] = activeBackend
      } catch (Exception ex) {
        println "    ${server.name}\t\tFAILED - ${ex.message}"
      }
    }
  }
  return ret
}

rebalanceNginxInstances = {

  def nginxToRebalance = []

  println "Returning NGINX instances to their preferred Tomcat, analysing cluster status"

  getAllNginxTargets().each { nginx, tomcatIp ->
    def currentTomcat = tomcatServerByIp(tomcatIp)
    println "${nginx.name} -> ${currentTomcat.name} [${nginx.preferredHost}]"
    if (currentTomcat.name != nginx.preferredHost) {
      nginxToRebalance << nginx
    }
  }

  println "Retargeting ${nginxToRebalance.size()} nginx instances"
  nginxToRebalance.each { server ->
    println "${server} to point at preferred ${server.preferredHost}"
    writeNginxConfig(server, tomcatServer(server.preferredHost))
  }

  println "Rebalanced NGINX load across Tomcat Cluster"
}

isolateTomcat = { tomcatServerHost, fallbackServerHost ->

  //analyse the cluster to see what points at the given tomcat
  def nginxToAlter = []

  //TODO, tell nagios that we are maintaining this server?

  println "Analysing cluster status"

  getAllNginxTargets().each { nginx, tomcatIp ->
    if (tomcatIp == tomcatServerHost.internal) {
      nginxToAlter << nginx
    }
  }
  //point each of these at the fallback tomcat
  println "Retargeting ${nginxToAlter.size()} nginx instances towards ${fallbackServerHost.name}"
  nginxToAlter.each { server ->
    writeNginxConfig(server, fallbackServerHost)
  }

  println "${tomcatServerHost.name} has been isolated from production access"

}
