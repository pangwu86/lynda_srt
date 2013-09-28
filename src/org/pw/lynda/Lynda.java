package org.pw.lynda;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.nutz.lang.Files;
import org.nutz.lang.util.Disks;
import org.nutz.log.Log;
import org.nutz.log.Logs;

public class Lynda {

    private static Log log = Logs.get();

    /**
     * 检查文件的video文件是否有丢失
     * 
     * @param srtDir
     * @return
     */
    public static boolean haveMissVideo(String srtDir) {
        log.infof("srt   dir : %s", srtDir);
        File sdir = new File(Disks.absolute(srtDir));
        Map<String, String> svMap = new HashMap<String, String>();
        for (File scdir : sdir.listFiles()) {
            if (scdir.getName().startsWith(".")) {
                continue;
            }
            if (scdir.isDirectory()) {
                for (File srtf : scdir.listFiles()) {
                    if (srtf.isHidden()) {
                        continue;
                    }
                    if (srtf.getName().endsWith(".srt")) {
                        svMap.put(srtf.getName(), null);
                        // 找一个名字一样, 后缀名不同的文件
                        Pattern vdoPattern = Pattern.compile("^("
                                                             + srtf.getName().replace(".srt", "")
                                                             + ")");
                        for (File vdof : scdir.listFiles()) {
                            if (!vdof.getName().endsWith(".srt")) {
                                if (vdoPattern.matcher(vdof.getName()).find()) {
                                    svMap.put(srtf.getName(), vdof.getName());
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return checkMissFile(svMap);
    }

    /**
     * 整合video与字幕, 放在一个文件夹中(放在字幕文件夹中)
     * 
     * @param videoDir
     * @param srtDir
     */
    public static void integrateVideoAndSrt(String videoDir, String srtDir) {
        File vdir = new File(Disks.absolute(videoDir));
        File sdir = new File(Disks.absolute(srtDir));
        if (vdir != null && sdir != null) {
            if (haveMissVideo(srtDir)) {
                log.infof("video dir : %s", videoDir);
                log.infof("srt   dir : %s", srtDir);
                Map<String, String> svMap = new HashMap<String, String>();
                File[] vcdirs = vdir.listFiles();
                File[] vfdirs = vdir.listFiles();
                boolean isAllDir = false;
                boolean isAllFile = false;
                int fnum = 0;
                int dnum = 0;
                for (File vd : vcdirs) {
                    if (vd.isFile()) {
                        fnum++;
                    } else {
                        dnum++;
                    }
                }
                isAllDir = dnum == vcdirs.length;
                isAllFile = fnum == vcdirs.length;
                log.infof("video dir is normal dir [%s], is video dir [%s]", isAllDir, isAllFile);
                // 遍历字幕目录
                for (File scdir : sdir.listFiles()) {
                    if (scdir.isHidden()) {
                        continue;
                    }
                    log.infof("    dir : %s", scdir.getName());
                    Pattern cnmPattern = Pattern.compile("^(\\s*[0-9]+\\.\\s*)([a-zA-z10-9\\s]*)$");
                    Matcher cnmMather = cnmPattern.matcher(scdir.getName());
                    if (cnmMather.find()) {
                        String cnm = cnmMather.group(2);
                        log.debugf("    srt dir nm : %s", cnm);
                        String[] cnmSplits = cnm.split(" ");
                        for (int i = 0; i < cnmSplits.length; i++) {
                            cnmSplits[i] = cnmSplits[i].toLowerCase();
                        }
                        if (isAllDir) {
                            // 从video目录中找到对应的目录
                            for (File vcdir : vcdirs) {
                                if (vcdir.isHidden()) {
                                    continue;
                                }
                                String vcnm = vcdir.getName().toLowerCase();
                                boolean allMatch = true;
                                for (int i = 0; i < cnmSplits.length; i++) {
                                    if (-1 == vcnm.indexOf(cnmSplits[i])) {
                                        allMatch = false;
                                        break;
                                    }
                                }
                                if (allMatch) {
                                    log.infof("    video dir '%s' has nm '%s'",
                                              vcdir.getName(),
                                              cnm);
                                    scanSrtFile(scdir.listFiles(), vcdir.listFiles(), svMap);
                                }
                            }
                        } else if (isAllFile) {
                            scanSrtFile(scdir.listFiles(), vfdirs, svMap);
                        }
                    } else {
                        log.warnf("  %s is not good dir", scdir.getName());
                    }
                }
                checkMissFile(svMap);
            }
        } else {
            log.errorf("video dir '%s' or srt dir '%s' is null, please check!", videoDir, srtDir);
        }
    }

    private static boolean checkMissFile(Map<String, String> svMap) {
        int sfileNum = 0;
        int vfileNum = 0;
        for (String srtNm : svMap.keySet()) {
            log.infof("srt : %s", srtNm);
            sfileNum++;
            if (svMap.get(srtNm) != null) {
                vfileNum++;
                log.infof("vdo : %s", svMap.get(srtNm));
            } else {
                log.info("vdo miss");
            }
        }
        boolean hasMiss = sfileNum == vfileNum;
        log.infof("Result : srt file %d, video file %d, %s",
                  sfileNum,
                  vfileNum,
                  (hasMiss ? "all file match!" : sfileNum - vfileNum + " files miss!"));
        return !hasMiss;
    }

    private static void scanSrtFile(File[] srtFiles, File[] videoFiles, Map<String, String> svMap) {
        // 检查每个srt文件
        for (File srtf : srtFiles) {
            if (srtf.isHidden()) {
                continue;
            }
            if (srtf.getName().endsWith(".srt")) {
                svMap.put(srtf.getName(), null);
                Pattern snmPattern = Pattern.compile("^(\\s*[0-9]+\\.\\s*)(.*)(\\.srt)$");
                Matcher snmMather = snmPattern.matcher(srtf.getName());
                if (snmMather.find()) {
                    String snm = snmMather.group(2);
                    log.infof("        srt file : %s", srtf.getName());
                    String[] snmSplits = snm.split(" ");
                    for (int i = 0; i < snmSplits.length; i++) {
                        snmSplits[i] = snmSplits[i].toLowerCase();
                    }
                    boolean vfound = false;
                    for (File videof : videoFiles) {
                        String vnm = videof.getName().toLowerCase();
                        boolean vmatch = true;
                        for (int i = 0; i < snmSplits.length; i++) {
                            if (-1 == vnm.indexOf(snmSplits[i])) {
                                vmatch = false;
                                break;
                            }
                        }
                        if (vmatch) {
                            vfound = true;
                            File vfile = new File(srtf.getParentFile(),
                                                  srtf.getName()
                                                      .replace(".srt",
                                                               videof.getName()
                                                                     .substring(videof.getName()
                                                                                      .lastIndexOf("."))));
                            svMap.put(srtf.getName(), vfile.getName());
                            if (vfile.exists()) {
                                log.infof("        vdo file found!");
                            } else {
                                log.infof("        vdo file : %s", vfile.getName());
                                Files.copy(videof, vfile);
                            }
                            break;
                        } else {
                            // log.errorf("video '%s' don't match '%s'",
                            // videof.getName(), snm);
                        }
                    }
                    if (!vfound) {
                        log.errorf("can't find srt file '%s' match video", srtf.getName());
                    }
                } else {
                    log.errorf("srt file is not good, %s", srtf.getName());
                }
            }
        }
    }

}
