package net.gazeplay.commons.utils.games;

import lombok.extern.slf4j.Slf4j;
import net.gazeplay.commons.configuration.ActiveConfigurationContext;
import net.gazeplay.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;

/**
 * Created by schwab on 23/12/2016.
 */
@Slf4j
public class Utils {

    public static InputStream getInputStream(String ressource) {
        log.debug("Try to play " + ressource);
        return ClassLoader.getSystemResourceAsStream(ressource);
    }

    /**
     * @return images directory for GazePlay : by default in the default directory of GazePlay, in a folder called files
     * but can be configured through option interface and/or GazePlay.properties file
     */

    private static String getFilesFolder() {

        Configuration config = ActiveConfigurationContext.getInstance();
        String filesFolder = config.getFileDir();

        log.info("filesFolder : " + filesFolder);
        return filesFolder;
    }

    /**
     * @return images directory for GazePlay : in the files directory another folder called images
     */

    public static File getBaseImagesDirectory() {
        File filesDirectory = new File(getFilesFolder());
        return new File(filesDirectory, "images");
    }

    public static File getImagesSubDirectory(String subfolderName) {
        File baseImagesDirectory = getBaseImagesDirectory();
        log.info("baseImagesDirectory {}", baseImagesDirectory);
        log.info("subfolderName {}", subfolderName);
        return new File(baseImagesDirectory, subfolderName);
    }

    /**
     * @return sounds directory for GazePlay : in the files directory another folder called sounds
     */

    public static String getSoundsFolder() {

        return getFilesFolder() + "sounds" + GazePlayDirectories.FILESEPARATOR;
    }

    /**
     * @return current date with respect to the format yyyy-MM-dd
     */
    public static String today() {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * @return current date with respect to the format dd/MM/yyyy
     */
    public static String todayCSV() {

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date date = new Date();
        return dateFormat.format(date);

    }

    /**
     * @return current time with respect to the format HH:MM:ss
     */
    public static String time() {

        DateFormat dateFormat = new SimpleDateFormat("HH:MM:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * @return current time with respect to the format yyyy-MM-dd-HH-MM-ss
     */
    public static String now() {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date();
        return dateFormat.format(date);

    }

    public static boolean copyFromJar(String filePath, String destinationPath) {
        InputStream sourceFile = null;
        OutputStream destinationFile = null;
        try {
            sourceFile = ClassLoader.getSystemResourceAsStream(filePath);
            if (sourceFile == null) {
                throw new IOException("Resource not found " + filePath);
            }
            destinationFile = new FileOutputStream(destinationPath);
            org.apache.commons.io.IOUtils.copy(sourceFile, destinationFile);
        } catch (IOException e) {
            log.error("Exception", e);
            return false; // Erreur
        } finally {
            IOUtils.closeQuietly(destinationFile);
            IOUtils.closeQuietly(sourceFile);
        }
        return true; // Résultat OK
    }

    public static String convertWindowsPath(String path) {

        path = path.replace("\\", "/");
        path = path.replaceAll("\\\\", "/");

        return path;
    }

    /**
     * @return true if the operating system is a Windows
     */
    public static boolean isWindows() {

        return System.getProperty("os.name").indexOf("indow") > 0;
    }

    public static void logSystemProperties() {

        Enumeration<?> E = System.getProperties().propertyNames();
        while (E.hasMoreElements()) {
            String element = (String) E.nextElement();
            log.info(String.format("%s: %s", element, System.getProperty(element)));
        }
    }
}
