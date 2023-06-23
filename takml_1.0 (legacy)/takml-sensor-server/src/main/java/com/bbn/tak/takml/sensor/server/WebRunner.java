package com.bbn.tak.takml.sensor.server;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.plus.jndi.EnvEntry;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.tak.takml.sensor.server.settings.Settings;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class WebRunner {
	private static final String ORG_ECLIPSE_JETTY_UTIL_LOG_CLASS = "org.eclipse.jetty.util.log.class";
	private static final String ORG_ECLIPSE_JETTY_LEVEL = "org.eclipse.jetty.LEVEL";
	// ** http://localhost:8081/DatabaseStatus
	// ** https://localhost:8080/DatabaseStatus
	private static final Logger LOGGER = LoggerFactory.getLogger(WebRunner.class);
	
	public static void main(String[] args) throws Exception {
		String jettyLoggingClassValue = System.getProperty(ORG_ECLIPSE_JETTY_UTIL_LOG_CLASS);
		String jettyLoggingLevelValue =  System.getProperty(ORG_ECLIPSE_JETTY_LEVEL);
		
		System.setProperty("bus.queueSize", "100000");
		
		if(jettyLoggingClassValue == null || jettyLoggingClassValue.isEmpty()) {
			System.setProperty(ORG_ECLIPSE_JETTY_UTIL_LOG_CLASS, "org.eclipse.jetty.util.log.StdErrLog");
		}
		
		if(jettyLoggingLevelValue == null || jettyLoggingLevelValue.isEmpty()) {
			System.setProperty(ORG_ECLIPSE_JETTY_LEVEL, "OFF");
		}
		
		String bannerText = FileUtils.readFileToString(new File("./banner.txt"), Charset.defaultCharset());
		LOGGER.info("\n\n" + bannerText + "\n\n");
		
		String settingsFile = "./settings.json";
		if(args.length > 0) {
			settingsFile = args[0];
		}
		
		String settingsJsonStr = FileUtils.readFileToString(new File(settingsFile), Charset.defaultCharset());
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		Settings settings = gson.fromJson(settingsJsonStr, Settings.class);
		
		if(args.length > 1 && args[1].equalsIgnoreCase("--show-settings")) {
			String printableSettings = gson.toJson(settings);
			LOGGER.debug(printableSettings);
		}
		
		Server server;
		Integer insecurePort = settings.getServer().getInsecurePort();
		if(insecurePort == null) {
			server = new Server();
		} else {
			server = new Server(insecurePort);
		}
		
		org.eclipse.jetty.webapp.Configuration.ClassList classlist = org.eclipse.jetty.webapp.Configuration.ClassList.setServerDefault(server);
	    classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration", "org.eclipse.jetty.plus.webapp.EnvConfiguration", "org.eclipse.jetty.plus.webapp.PlusConfiguration");
	    classlist.addBefore("org.eclipse.jetty.webapp.JettyWebXmlConfiguration", "org.eclipse.jetty.annotations.AnnotationConfiguration");

	    WebAppContext webapp = new WebAppContext();
	    webapp.setInitParameter("bus.queueSize", "100000");
	    Map<String, Object> frostParams = settings.getFrostServer().getParameters();
	    for(String key : frostParams.keySet()) {
	    	webapp.setInitParameter(key, frostParams.get(key).toString());
		}
	    
	    webapp.setContextPath("/");
	    webapp.setAttribute("org.eclipse.jetty.server.webapp.WebInfIncludeJarPattern", ".*WEB-INF/lib/FROST.*");
	    webapp.setWar("./libs/FROST-Server.HTTP-1.12.0.war");
	    server.setHandler(webapp);
	    
	    // JNDI: jdbc/sensorThings
	    PGSimpleDataSource dataSource = new PGSimpleDataSource();
	    dataSource.setUrl(settings.getDatabase().getUrl());
	    dataSource.setUser(settings.getDatabase().getUser());
	    dataSource.setPassword(settings.getDatabase().getPassword());
	    
	    EnvEntry sensorThingsDataSourceEntry = new EnvEntry(server, "jdbc/sensorThings", dataSource, false);
	    
	    NCSARequestLog requestLog = new NCSARequestLog("./jetty-yyyy_mm_dd.request.log");
	    requestLog.setAppend(true);
	    requestLog.setExtended(true);
	    requestLog.setLogTimeZone("GMT");
	    requestLog.setLogLatency(true);
	    requestLog.setRetainDays(90);

	    server.setRequestLog(requestLog);
	    
	    configureTLS(server, settings.getServer().getKeystorePath(), settings.getServer().getKeystorePass(), settings.getServer().getTruststorePath(), settings.getServer().getTruststorePass(), settings.getServer().getSecurePort());
	    
	    server.start();
	    server.join();
	}
	
	private static void configureTLS(Server server, String keystorePath, String keystorePassword, String truststorePath, String truststorePassword, int port) {
        SslContextFactory contextFactory = new SslContextFactory();
        contextFactory.setKeyStorePath(keystorePath);
        contextFactory.setKeyStorePassword(keystorePassword);
        contextFactory.setNeedClientAuth(true);
        contextFactory.setTrustStorePath(truststorePath);
        contextFactory.setTrustStorePassword(truststorePassword);
        
        SslConnectionFactory sslConnectionFactory =  new SslConnectionFactory(contextFactory,
                org.eclipse.jetty.http.HttpVersion.HTTP_1_1.toString());
        
        HttpConfiguration config = new HttpConfiguration();
        config.setSecureScheme("https");
        config.setSecurePort(port);
        HttpConfiguration sslConfiguration = new HttpConfiguration(config);
        sslConfiguration.addCustomizer(new SecureRequestCustomizer());
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(sslConfiguration);
                ServerConnector connector = new ServerConnector(server, sslConnectionFactory, httpConnectionFactory);
        connector.setPort(port);
        server.addConnector(connector);
    }
}
