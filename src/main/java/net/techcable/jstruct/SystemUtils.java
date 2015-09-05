package net.techcable.jstruct;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemUtils {

    // Java versions

    public static boolean isJavaVersionAtLeast(int requiredVersion) {
        String versionString = "1." + requiredVersion;
        return getVersion().compareTo(versionString) >= 0;
    }

    private static String getVersion() {
        String version = System.getProperty("java.version");
        Preconditions.checkNotNull(version, "Null java.version");
        return version;
    }

    // JVM vendors

    public static boolean isSunOrOracleVm() {
        String vendor = getVendor();
        return isOracleVM(vendor) || isSunVM(vendor);
    }

    public static boolean isOracleVM() {
        return isOracleVM(getVendor());
    }

    private static boolean isOracleVM(String vendor) {
        return vendor.startsWith("oracle");
    }

    private static String getVendor() {
        String vendor = System.getProperty("java.vm.vendor");
        Preconditions.checkNotNull(vendor, "Null java.vm.vendor");
        vendor = vendor.toLowerCase();
        return vendor;
    }

    public static boolean isSunVM() {
        return isSunVM(getVendor());
    }

    public static boolean isSunVM(String vendor) {
        return vendor.startsWith("sun");
    }

}
