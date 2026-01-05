package com.bbn.certPackager;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class CertPackager {

    public static final String DEFAULT_PROTOCOL = "ssl";
    public static final String DEFAULT_PORT = "8089";
    public static final String DEFAULT_PASSWORD = "atakatak";
    public static final String DEFAULT_TARGET_DIRECTORY = ".";

    public static final String FOLDER_NAME_MANIFEST = "MANIFEST";
    public static final String FILE_NAME_MANIFEST = "manifest.xml";
    public static final String FILE_NAME_SECURE_PREF = "secure.pref";


    public static final String ARGUMENT_PROTOCOL = "protocol";
    public static final String ARGUMENT_PORT = "port";
    public static final String ARGUMENT_PASSWORD = "password";
    public static final String ARGUMENT_TARGET_DIRECTORY = "targetDirectory";

    public static final String EXTENSION_P12 = ".p12";
    public static final String EXTENSION_ZIP = ".zip";
    
    public static final String PREFS_VELOCITY_KEY_SERVER_NAME = "serverName";
    public static final String PREFS_VELOCITY_KEY_IP = "ip";
    public static final String PREFS_VELOCITY_KEY_PORT = "port";
    public static final String PREFS_VELOCITY_KEY_PROTOCOL = "protocol";
    public static final String PREFS_VELOCITY_KEY_PASSWORD = "password";
    public static final String PREFS_VELOCITY_KEY_TRUST_STORE_LOCATION = "trustStoreLocation";
    public static final String PREFS_VELOCITY_KEY_CERTIFICATE_LOCATION = "certificateLocation";

    public static final String MANIFEST_VELOCITY_KEY_TRUST_STORE_NAME = "trustStoreName";
    public static final String MANIFEST_VELOCITY_KEY_CERTIFICATE_NAME = "certName";

    public static final String USAGE_MESSAGE="Usage: serverName ip trustStore\n\n" +
            "Makes mission package for each .p2 file in directory, except for the specified trust store.\n" +
            "Options:\n" +
            "--protocol (defaults to ssl)\n" +
            "--port (defaults to 443)\n" +
            "--password (defaults to atakatak)\n" +
            "--targetDirectory (defaults to current directory)\n\n" +
            "Note that if your arguments contain spaces, you may need to put a backslash (\"\\\") in front of them.";


    public static void main(String[] argArray) {
        Args args=new Args(argArray);

        File targetDir = new File(args.getTargetDirectory());
        File trustStore=new File(targetDir, args.getTruststoreName());
        //loop over all the .p12 files that aren't the trust store
        int numZipsCreated=0;
        for (File cert : targetDir.listFiles(
                pathname -> pathname.getName().endsWith(EXTENSION_P12)
                &&!pathname.equals(trustStore))) {



            //create cert folder
            String certName = cert.getName();
            String nameWithoutP12= certName.substring(0, certName.length()-4);
            File zipFile = new File(targetDir, nameWithoutP12 + EXTENSION_ZIP);
            ZipOutputStream zos= null;
            try {
                try {
                    zos = new ZipOutputStream(new FileOutputStream(zipFile));
                    //copy in certificate.  Note this is slightly confusing since the folder everything goes into has
                    //the same name as the cert, eg cert1/cert1.p12
                    zos.putNextEntry(new ZipEntry(nameWithoutP12+File.separator+certName));
                    copyFileToZipStream(cert, zos);
                    zos.closeEntry();

                    //copy in the trust store
                    String trustStoreName = trustStore.getName();
                    zos.putNextEntry(new ZipEntry(nameWithoutP12+File.separator+ trustStoreName));
                    copyFileToZipStream(trustStore, zos);
                    zos.closeEntry();

                    //create a template based manifest, then put it in under a subfolder called MANIFEST, eg cert1/MANIFEST/manifest.xml
                    zos.putNextEntry(new ZipEntry(nameWithoutP12+File.separator+FOLDER_NAME_MANIFEST+File.separator+FILE_NAME_MANIFEST));
                    createManifest(certName, trustStoreName, zos);
                    zos.closeEntry();

                    //create a template based secure.pref
                    zos.putNextEntry(new ZipEntry(nameWithoutP12+File.separator+FILE_NAME_SECURE_PREF));
                    createSecurePref(args, zos, certName, trustStoreName);
                    zos.closeEntry();

                } catch (IOException e) {
                    System.err.println("Could not zip "+zipFile);
                    e.printStackTrace();
                    continue;
                }finally {
                    zos.close();
                }
            } catch (IOException e) {
                System.err.println("Could not zip "+zipFile);
                e.printStackTrace();
                continue;
            }
            numZipsCreated++;
        }

        System.out.println("Success.  Created "+numZipsCreated+" files in "+targetDir);
    }

    private static void createSecurePref(Args args, OutputStream os, String certFileName, String trustStoreFileName) throws IOException {

        String templateFile = "secure.pref.vm";

        VelocityEngine ve = new VelocityEngine();
        ve.init();
        Template t = createVelocityTemplate(templateFile, ve, "Secure");
        VelocityContext vc = new VelocityContext();
        vc.put(PREFS_VELOCITY_KEY_SERVER_NAME, args.getServerName());
        vc.put(PREFS_VELOCITY_KEY_IP, args.getIp());
        vc.put(PREFS_VELOCITY_KEY_PORT, args.getPort());
        vc.put(PREFS_VELOCITY_KEY_PROTOCOL, args.getProtocol());
        vc.put(PREFS_VELOCITY_KEY_PASSWORD, args.getPassword());
        vc.put(PREFS_VELOCITY_KEY_TRUST_STORE_LOCATION, DEFAULT_CERT_PATH()+File.separator+trustStoreFileName);
        vc.put(PREFS_VELOCITY_KEY_CERTIFICATE_LOCATION, DEFAULT_CERT_PATH()+File.separator+certFileName);

        OutputStreamWriter osw = new OutputStreamWriter(os);
        t.merge(vc, osw);
        osw.flush();
    }

	private static String DEFAULT_CERT_PATH() {
		return "cert";
	}


    private static void createManifest(String certName, String trustStoreName, ZipOutputStream zos) throws IOException {
        VelocityEngine ve = new VelocityEngine();
        ve.init();

        Template t = createVelocityTemplate("manifest.xml.vm", ve, "manifest");

        VelocityContext vc = new VelocityContext();
        vc.put(MANIFEST_VELOCITY_KEY_TRUST_STORE_NAME, trustStoreName);
        vc.put(MANIFEST_VELOCITY_KEY_CERTIFICATE_NAME, certName);

        OutputStreamWriter osw = new OutputStreamWriter(zos);
        t.merge(vc, osw);
        osw.flush();
    }

    private static Template createVelocityTemplate(String templateFileName, VelocityEngine ve, String templateName) {
        RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
        Template template=null;
        try {
            InputStreamReader reader = new InputStreamReader(CertPackager.class.getClassLoader().getResourceAsStream(templateFileName));

            template = new Template();
            template.setRuntimeServices(runtimeServices);

            /*
             * The following line works for Velocity version up to 1.7
             * For version 2, replace "Template name" with the variable, template
             */

            template.setData(runtimeServices.parse(reader, templateName));
            template.initDocument();
        } catch (ParseException e) {
            System.err.println("Could not parse "+templateFileName);
            e.printStackTrace();
        }
        return template;

    }



    /**
     * Takes a file and an outputStream.  Copies the copies of the file into the outputstream without closing the output stream.
     * (This is what we want because we're writing a whole series of files to the same Zip stream.)
     */
    private static void copyFileToZipStream(File source, OutputStream dest) throws IOException {
        InputStream is = null;
        try {
            is = new FileInputStream(source);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                dest.write(buffer, 0, length);
            }
        } finally {
            is.close();
        }
    }

    private static class Args {
        private String serverName;
        private String ip;
        private String truststoreName;
        private String protocol = DEFAULT_PROTOCOL;
        private String port = DEFAULT_PORT;
        private String password = DEFAULT_PASSWORD;
        private String targetDirectory = DEFAULT_TARGET_DIRECTORY;


        public Args(String[] argArray) {

            //Since optional -- arguments can be stuck in at any point whe only increment this counter for the other ones
            int nonOptionalArgumentCounter = 0;
            for (String arg : argArray) {
                if (arg.startsWith("--")) {

                    if (!arg.contains("=")) {
                        System.out.println("-- Param expects = ("+arg+")");
                        System.exit(1);
                    }

                    //optional param
                    String[] splitParam = arg.substring(2).split("=");
                    String paramName = splitParam[0];
                    String paramValue = splitParam[1];

                    //switch on param name
                    if (ARGUMENT_PROTOCOL.equalsIgnoreCase(paramName)) {
                        protocol = paramValue;
                    } else if (ARGUMENT_PORT.equalsIgnoreCase(paramName)) {
                        port = paramValue;
                    } else if (ARGUMENT_PASSWORD.equalsIgnoreCase(paramName)) {
                        password = paramValue;
                    } else if (ARGUMENT_TARGET_DIRECTORY.equalsIgnoreCase(paramName)) {
                        targetDirectory = paramValue;
                    }

                    //skip the rest of the loop so we don't increment our non-optional argument counter
                    // on an optional argument
                    continue;
                }

                switch (nonOptionalArgumentCounter) {
                    case 0:
                        serverName = arg;
                        break;
                    case 1:
                        ip = arg;
                        break;
                    case 2:
                        truststoreName = arg;
                        if (!truststoreName.endsWith(".p12")) {
                            truststoreName=truststoreName+".p12";
                        }
                        if (truststoreName.contains("/")){
                            //if they passed in a path, just use the file name.
                            truststoreName= truststoreName.substring(truststoreName.lastIndexOf('/')+1);
                        }
                        break;
                    default:
                        System.out.println("Unexpected argument " + arg+"\n\n"+USAGE_MESSAGE);
                        System.exit(1);
                }
                nonOptionalArgumentCounter++;
            }
            if (nonOptionalArgumentCounter < 3) {
                System.out.println("Not enough arguments"+"\n\n"+USAGE_MESSAGE);
                System.exit(1);
            }
        }


        public String getServerName() {
            return serverName;
        }

        public String getIp() {
            return ip;
        }

        public String getTruststoreName() {
            return truststoreName;
        }

        public String getProtocol() {
            return protocol;
        }

        public String getPort() {
            return port;
        }

        public String getPassword() {
            return password;
        }

        public String getTargetDirectory() {
            return targetDirectory;
        }

    }



}
