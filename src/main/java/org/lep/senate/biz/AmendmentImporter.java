package org.lep.senate.biz;

import javafx.util.Pair;
import org.lep.senate.dao.SenateDAO;
import org.lep.senate.loader.document.AmendmentIndexDocument;
import org.lep.senate.loader.document.DocumentLoader;
import org.lep.senate.model.Senator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map.Entry;

public class AmendmentImporter {
    private static final Logger logger = LoggerFactory.getLogger(AmendmentImporter.class);

    private static final SenateDAO dao = new SenateDAO();

    public static void main(String[] args) {
        dao.resetAmdtSponsors();

        for(int c = 97; c < 114; c++) {
            try {
                processCongress(c);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static void processCongress(int congressNum) throws FileNotFoundException {
        logger.info("processing amendments for congress " + congressNum);
        File[] amdtIndexFiles = DocumentLoader.getAmdtFiles(congressNum);

        for(File f : amdtIndexFiles) {
            try {
                logger.info("\tprocessing amendment index page {}", f.getName());
                AmendmentIndexDocument amdtIndex = new AmendmentIndexDocument(f);
                for(Entry<String, Pair<Integer, String[]>> amdtSponsor : amdtIndex.getAmdtBillSponsors().entrySet()) {
                    String amdtNum = amdtSponsor.getKey();
                    Pair<Integer, String[]> val = amdtSponsor.getValue();
                    Integer billNum = val.getKey();
                    String[] sponsor = val.getValue();
                    Senator s = dao.getSenator(sponsor);
                    dao.createAmdtSponsor(congressNum, amdtNum, s.getId(), billNum);
                }
            } catch(Exception e) {
                logger.error("Failed to process congress {} amendment index page {}: {}",
                        congressNum, f.getName(), e.getMessage());
            }
        }
    }
}
