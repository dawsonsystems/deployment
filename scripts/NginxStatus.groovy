import com.jcraft.jsch.Session
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Channel

includeTargets << grailsScript("_GrailsInit")
includeTargets << new File("${basedir}/scripts/_Env.groovy")
includeTargets << new File("${basedir}/scripts/_DeployCommon.groovy")

target(default: "Show the current NGINX status") {
  depends(password, passphrase)

  println "Analysing cluster status"

  getAllNginxTargets().each { nginx, tomcatIp ->
    def currentTomcat = tomcatServerByIp(tomcatIp)
    println "${nginx.name}\t${currentTomcat.name}"
  }
}
