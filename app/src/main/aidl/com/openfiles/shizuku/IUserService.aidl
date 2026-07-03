package com.openfiles.shizuku;

interface IUserService {
    /** Runs `pm uninstall <packageName>` in the Shizuku-granted process. Returns the process exit code (0 = success). */
    int uninstallPackage(String packageName);

    void destroy();
}
