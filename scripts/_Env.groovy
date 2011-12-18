
import grails.spring.BeanBuilder
import com.jcraft.jsch.JSch
import com.jcraft.jsch.UserInfo
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsApplication
/**
 * Server names/ external IP/ internal IP
 */
nginx = [
       // host(name: "mycenae", external: "111.111.111.1111", internal: "1.1.1.1", preferredHost:"mycenae")

]

tomcat = [
        host(name: "demo", external: "mycenae", internal: "mycenae"),
        host(name: "cms1", external: "cms1.dawsonsystems.com", internal: "cms1.dawsonsystems.com"),
]

deployServers = tomcat.findAll { it.name != "demo" }

tomcatServer =  tomcat

tomcatServerByIp = { String ip ->
  return tomcat.find { it.internal == ip}
}

nginxServer = { String name ->
  return nginx.find { it.name == name}
}

if (System.properties["keyfile"] || System.getenv("JENKINS_KEYFILE")) {
  keyFile=System.getenv("JENKINS_KEYFILE") ?: System.properties["keyfile"]
} else {
  keyFile="${System.properties['user.home']}/.ssh/id_rsa"
}

if (System.properties["SCRIPTING_PRODUCTION"] == "true" || System.getenv("SCRIPTING_PRODUCTION") == "true") {
  productionMode=true
  println "Operating in PRODUCTION mode, changes may be made to the production systems"
} else {
  productionMode = false
}

nagiosTemp = File.createTempFile("nagios", "nagiosLocalConfig")
nagiosTemp.delete()
nagiosTemp.mkdirs()

def ant = new AntBuilder()

//Allow referencing src/groovy
this.class.classLoader.addURL(grailsSettings.classesDir.toURI().toURL())

/**
 * Targets that can be depended on in scripts using the syntax
 *
 * depends(password)
 *
 */
target(password: "Obtain the password from the user") {
  def password = System.getenv("SYSTEM_PASSWORD") ?: System.properties["SYSTEM_PASSWORD"]
  if (!password) {
    promptForValue("password")
  } else {
    getBinding().setProperty("password", password)
  }
}

target(passphrase: "Obtain passphrase from the user") {
  def passphrase = System.getenv("SYSTEM_PASSPHRASE") ?: System.properties["SYSTEM_PASSPHRASE"]
  
  if(passphrase == null) {
    promptForValue("passphrase")
  } else {
    getBinding().setProperty("passphrase", passphrase)
  }
}
//
//target(connectProductionMongo: "Open an SSH tunnel to Mongo1, allowing querying the production database") {
//  println "Opening SSH tunnel to Mongo1"
//
//  jsch = new JSch()
//  jsch.addIdentity(keyFile, passphrase)
//
//  sshSession = jsch.getSession("admin", "174.143.26.62", (int) 30000)
//  sshSession.setConfig("StrictHostKeyChecking", "no")
//  println "SSH Connection made, opening tunnel"
//
//  UserInfo ui= ["getPassword":{ password },
//    promptYesNo: { return false },
//    promptPassword: { return false },
//    promptPassphrase: { return false } ] as UserInfo
//
//  sshSession.setUserInfo(ui);
//  sshSession.connect();
//
//  def assigned = sshSession.setPortForwardingL(27027, "localhost", 27017)
//
//  println "Tunnel open on port 27027, production mongo now accessible locally"
//}

//target(connectDemoMongo: "Open an SSH tunnel to Demo, allowing querying and importing data to integration database") {
//  println "Opening SSH tunnel to Demo"
//
//  jsch = new JSch()
//  jsch.addIdentity(keyFile, passphrase)
//
//  sshSession = jsch.getSession("admin", "174.143.255.88", (int) 30000)
//  sshSession.setConfig("StrictHostKeyChecking", "no")
//  println "SSH Connection made, opening tunnel"
//
//  UserInfo ui= ["getPassword":{ password },
//    promptYesNo: { return false },
//    promptPassword: { return false },
//    promptPassphrase: { return false } ] as UserInfo
//
//  sshSession.setUserInfo(ui);
//  sshSession.connect();
//
//  def assigned = sshSession.setPortForwardingL(27027, "localhost", 27017)
//
//  println "Tunnel open on port 27027, Demo mongo now accessible locally"
//}

//target(simpleContext: "Simple application context, containing datasource and a few services.") {
//  //Some basic wiring, we don't want to initialise the whole application
//  def bb = new BeanBuilder()
//
//  int dbPort = 27017
//  String database = "shoutback-dev"
//  if (productionMode) {
//    dbPort = 27027
//    database = "shoutback"
//  }
//
//  bb.beans() {
//    mongoDb(Mongo, "127.0.0.1", dbPort)
//    db(classLoader.loadClass("com.clicknshout.DbFactoryBean")) {
//       bean ->
//      mongo = ref(mongoDb)
//      name = database
//    }
//
//    dealScheduleService(classLoader.loadClass("com.clicknshout.services.DealScheduleService")) { bean ->
//      bean.autowire = "byName"
//    }
//    publisherService(classLoader.loadClass("com.clicknshout.services.PublisherService")) { bean ->
//      bean.autowire = "byName"
//    }
//    accountService(classLoader.loadClass("com.clicknshout.services.AccountService")) { bean ->
//      bean.autowire = "byName"
//    }
//    reportingService(classLoader.loadClass("com.clicknshout.report.ReportingService")) { bean ->
//      bean.autowire = "byName"
//    }
//    orderService(classLoader.loadClass("com.clicknshout.services.OrderService")) { bean ->
//      bean.autowire = "byName"
//    }
//    dealQueryService(classLoader.loadClass("com.clicknshout.query.DealQueryService")) { bean ->
//      bean.autowire = "byName"
//    }
//    adjustmentService(classLoader.loadClass("com.clicknshout.services.AdjustmentService")) { bean ->
//      bean.autowire = "byName"
//    }
//  }
//
//  ctx = bb.createApplicationContext()
//
//
//  ApplicationHolder.application = [
//        "getMainContext"  : { ctx }
//  ] as GrailsApplication
//
//  getBinding().setProperty("db", ctx.getBean("db"))
//
//
//  postWireService("accountService", ctx)
//  postWireService("publisherService", ctx)
//  postWireService("dealScheduleService", ctx)
//  postWireService("orderService", ctx)
//  postWireService("dealQueryService", ctx)
//  postWireService("reportingService", ctx)
//  postWireService("adjustmentService", ctx)
//}
//
//postWireService = { name, ctx ->
//
//  getBinding().setProperty(name, ctx.getBean(name))
//
//  initLogger(getBinding().getProperty(name))
//}
//
//initLogger = { obj ->
//  obj.metaClass.getLog = { ["debug": { println "DEBUG:$it"}, "info":{ println "INFO:$it"}, "error":{ println "ERROR:$it"}] }
//}

/**
 * Some usefult methods/ functions
 */

def host(Map params) {
  return params
}

promptForValue = { String value, String message=null, Class type=null ->
  //TODO, allow to be passed as a command line arg
  def propertyValue = System.getenv(value) ?: System.properties[value]

  if (propertyValue == null) {
    if (message == null) {
      message = "${value} not specified. Please enter:"
    }
    ant.input(addProperty: value, message: message)
    if (type) {
      getBinding().setProperty(value, ant.antProject.properties."${value}".asType(type))
    } else {
      getBinding().setProperty(value, ant.antProject.properties."${value}")
    }
  } else {
    if (type) {
      getBinding().setProperty(value, propertyValue.asType(type))
    } else {
      println "Setting $value == $propertyValue"
      getBinding().setProperty(value, propertyValue)
    }
  }
}

closeSSH = {
  jsch.removeSession(sshSession)
  println "SSH Tunnel closed"
}

allowFail =  { Closure closure ->
  try {

    closure()
  } catch (Exception ex) {
    println ex.message
  }

}

execRemote = { String command ->
  ant.sshexec(host: currentServer.external,
          keyfile: keyFile,
          passphrase:passphrase,
          port: 22,
          username: "david",
          trust: "yes",
          command: "bash -c \"echo ${password} | sudo -S ${command}\"")
}

execRemoteInDir = { String command, String path ->
  ant.sshexec(host: currentServer.external,
          keyfile: keyFile,
          passphrase:passphrase,
          port: 22,
          username: "david",
          trust: "yes",
          command: "bash -l -c \"export JAVA_HOME=/usr/java/jdk1.6.0_18 && export GRAILS_HOME=/home/admin/grails && PATH=\$PATH:\$GRAILS_HOME/bin && cd ${path} && ${command}\"")
}

copy = { def localFile, def remoteFile ->

  def file = new File(localFile)

  ant.scp(host: currentServer.external,
          port: 22,
          localFile: localFile,
          keyfile: keyFile,
          passphrase:passphrase,
          trust: true,
          sftp:true,
          verbose:true,
          remoteTodir: "david@${currentServer.external}:/home/david"
  )


  execRemote("mv ${file.name} ${remoteFile}")
}
