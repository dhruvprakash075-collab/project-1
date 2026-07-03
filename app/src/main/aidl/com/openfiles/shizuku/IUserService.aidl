package com.openfiles.shizuku;

interface IUserService {
    /** Runs `pm uninstall <packageName>` and returns exit code plus command output. */
    String uninstallPackage(String packageName);

    void destroy();
}
