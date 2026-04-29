package com.buldreinfo.jersey.jaxb.batch.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

public class DataSftpDownloadTask {
    private static final Logger logger = LogManager.getLogger();
    private final String sshHost;
    private final String sshUser;
    private final String sshKeyPath;
    private final Path dbBasePath;
    private final Path infraPath;
    private final String remoteBackupDir;

    public DataSftpDownloadTask(String sshHost, String sshUser, String sshKeyPath, Path dbBasePath, Path infraPath, String remoteBackupDir) {
        this.sshHost = sshHost;
        this.sshUser = sshUser;
        this.sshKeyPath = sshKeyPath;
        this.dbBasePath = dbBasePath;
        this.infraPath = infraPath;
        this.remoteBackupDir = remoteBackupDir;
    }

    public void run() {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        Path localYearDbPath = dbBasePath.resolve(currentYear);

        try (SSHClient ssh = new SSHClient()) {
            Files.createDirectories(localYearDbPath);
            Files.createDirectories(infraPath);
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(sshHost);
            ssh.authPublickey(sshUser, sshKeyPath);

            try (SFTPClient sftp = ssh.newSFTPClient()) {
                logger.info("Scanning for the latest backups in {}...", remoteBackupDir);
                
                List<RemoteResourceInfo> remoteFiles = sftp.ls(remoteBackupDir);
                RemoteResourceInfo latestDb = null;
                RemoteResourceInfo latestProxy = null;

                for (RemoteResourceInfo file : remoteFiles) {
                    String name = file.getName();
                    if (name.endsWith(".climbing.sql.gz")) {
                        if (latestDb == null || name.compareTo(latestDb.getName()) > 0) {
                            latestDb = file;
                        }
                    } else if (name.endsWith(".varden-proxy.tar.gz")) {
                        if (latestProxy == null || name.compareTo(latestProxy.getName()) > 0) {
                            latestProxy = file;
                        }
                    }
                }
                if (latestDb != null) {
                    syncFile(sftp, latestDb.getName(), localYearDbPath.resolve(latestDb.getName()), latestDb.getAttributes().getSize());
                }
                if (latestProxy != null) {
                    syncFile(sftp, latestProxy.getName(), infraPath.resolve(latestProxy.getName()), latestProxy.getAttributes().getSize());
                }
            }
        } catch (IOException e) {
            logger.error("SFTP Task failed: {}", e.getMessage(), e);
        }
    }

    private void syncFile(SFTPClient sftp, String remoteName, Path localPath, long remoteSize) throws IOException {
        if (Files.exists(localPath) && Files.size(localPath) == remoteSize) {
            logger.debug("Latest file {} already downloaded.", remoteName);
            return;
        }
        logger.info("Downloading latest: {} to {}", remoteName, localPath);
        sftp.getFileTransfer().setPreserveAttributes(false);
        sftp.get(remoteBackupDir + "/" + remoteName, localPath.toString());
    }
}