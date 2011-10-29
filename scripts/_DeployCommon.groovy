import com.jcraft.jsch.JSch
import com.jcraft.jsch.UserInfo
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Channel
import com.jcraft.jsch.Session

includeTargets << new File("${basedir}/scripts/_Env.groovy")
includeTargets << new File("${basedir}/scripts/_NginxCommon.groovy")

//if (System.properties["keyfile"]) {
//  keyFile=System.properties["keyfile"]
//} else {
//  keyFile="${System.properties['user.home']}/.ssh/id_rsa"
//}

target(connectBuild: "Connect to the build server") {
  depends(password)
  //TODO, check if we are already local...

  println "Opening SSH connection to"

  jsch = new JSch()
  sshSession = jsch.getSession("admin", buildServer.external, (int) 30000)

  println "SSH Connection made, opening tunnel"

  UserInfo ui= ["getPassword":{ password },
    promptYesNo: { return true },
    promptPassword: { return true },
    promptPassphrase: { return false } ] as UserInfo

  sshSession.setUserInfo(ui);
  sshSession.connect();

  currentServer = buildServer

  //def assigned = sshSession.setPortForwardingL(27017, "localhost", 27017)

  println "Tunnel open on port 27017, production mongo now accessible locally"
}

target(gatherSingleServerDetails: "Connect to a single server and gather build info") {

  promptForValue("productionServer", "Specify a single production server to connect to, currently available ${deployServers.collect{ it.name } }")

  def serverInfo = gatherServerInfo(productionServer)

  live = serverInfo.live
  staged = serverInfo.staged
  rollback = serverInfo.rollback
}

gatherServerInfo = { name ->
  def serverInfo

  ssh(deployServers.find { it.name == productionServer} ) { Session session ->
    try {
      Channel channel=session.openChannel("sftp");
      channel.connect();
      ChannelSftp sftp =(ChannelSftp)channel;

      def files = sftp.ls("/home/admin/deploys")

      files = files.findAll { it.toString().contains("war") }.collect { it.filename }

      serverInfo = [live : files.find { it.contains("LIVE") },
              rollback : files.find { it.contains("ROLLBACK") },
              staged : files.find { it.contains("STAGE") }]

    } catch (Exception ex) {
      println "Cannot connect to prod server... : ${ex.message}"
      System.exit(0)
    }
  }
  return serverInfo
}

ssh = { host, Closure exec ->

  jsch = new JSch()
  jsch.addIdentity(keyFile, passphrase)

  sshSession = jsch.getSession("admin", host.external, (int) 30000)
  sshSession.setConfig("StrictHostKeyChecking", "no")

  UserInfo ui= ["getPassword":{ password },
    promptYesNo: { return false },
    promptPassword: { return false },
    promptPassphrase: { return false } ] as UserInfo

  sshSession.setUserInfo(ui);
  sshSession.connect();

  currentServer = host

  exec(sshSession)

  sshSession.disconnect()
}


doDeploy = { targetTomcat ->

  currentServer = targetTomcat

  println "Installing current tomcat control script"

  copy("src/system/deployment/restartTomcatAndWait.bash", "/home/david/restartTomcat.sh")

  println "Copying war file to demo server"

  copy("${deployWar}", "${targetWar}")

  println "Restarting Tomcat"

  execRemote("chmod 755 /home/david/restartTomcat.sh")
  execRemote("/home/david/restartTomcat.sh")


}