# The proguard configuration file for the following section is /home/brandon/TAKMaps/atak-civ/apps/takml-android/app/proguard-gradle.txt
################################################################################################
## Skip down to the 'User Section'
## Messing with any items in the 'System Section' will void the warranty
################################################################################################


################################################################################################
################################################################################################
## System Section
################################################################################################
################################################################################################


-dontskipnonpubliclibraryclasses
-dontshrink
-dontoptimize
-ignorewarnings

############### ACRA specifics
# we need line numbers in our stack traces otherwise they are pretty useless
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-applymapping <atak.proguard.mapping>

-keepattributes *Annotation*
-keepattributes Signature, InnerClasses


-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}



# Preserve all native method names and the names of their classes.
-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers class * {
    @org.simpleframework.xml.* *;
}


# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}



-keep class * extends transapps.maps.plugin.tool.Tool {
}
-keep class * implements transapps.maps.plugin.lifecycle.Lifecycle {
}

# overcome an existing bug in the gradle subsystem (3.5.x)
-keep class module-info

################################################################################################
## Messing with any settings above this line will void the Warranty
################################################################################################



################################################################################################
################################################################################################
## User Section
################################################################################################
################################################################################################


################################################################################################
## Please change PluginTemplate to accurately reflect the name of your plugin
################################################################################################

-repackageclasses atakplugin.TakMlFramework
-keep class org.apache.log4j.** {*;}
-keep class io.netty.** {*;}

################################################################################################
## below you can add any rules specific to your plugin limited to dontwarn and keep directives
################################################################################################
# End of content from /home/brandon/TAKMaps/atak-civ/apps/takml-android/app/proguard-gradle.txt
# The proguard configuration file for the following section is /home/brandon/TAKMaps/atak-civ/apps/takml-android/app/proguard-rules.pro
-printconfiguration fullconfig.pro
# End of content from /home/brandon/TAKMaps/atak-civ/apps/takml-android/app/proguard-rules.pro
# The proguard configuration file for the following section is /home/brandon/TAKMaps/atak-civ/apps/takml-android/app/build/intermediates/aapt_proguard_file/civRelease/aapt_rules.txt
-keep class com.bbn.takml.framework.TakMlFramework { <init>(); }
-keep class com.atakmap.android.gui.PanEditTextPreference { <init>(...); }

-keep class com.atakmap.android.gui.PanPreference { <init>(...); }


# End of content from /home/brandon/TAKMaps/atak-civ/apps/takml-android/app/build/intermediates/aapt_proguard_file/civRelease/aapt_rules.txt
# The proguard configuration file for the following section is <unknown>
-ignorewarnings
# End of content from <unknown>