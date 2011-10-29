includeTargets << grailsScript("_GrailsInit")
includeTargets << new File("${basedir}/scripts/_DeployCommon.groovy")

target(default: "Deploy a build live") {
  depends(password, passphrase)

  //for each tomcat in turn
  deployServers.each { tomcat ->
    println "Deploying to ${tomcat.name}"

    //to implement when there are more systems..
//    def fallbackTomcat = tomcatServer("tomcatprimary")
//    if (tomcat.name == "tomcatprimary") {
//      fallbackTomcat = tomcatServer("tomcathotswap")
//    }
//
//    println "  transferring load to ${fallbackTomcat.name}"
//    isolateTomcat(tomcat, fallbackTomcat)

    doDeploy(tomcat)
  }

//  rebalanceNginxInstances()
}
