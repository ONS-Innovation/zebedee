package com.github.onsdigital.zebedee.data.processing;

import com.github.onsdigital.zebedee.content.page.statistics.data.timeseries.TimeSeries;
import com.github.onsdigital.zebedee.data.processing.setup.DataIndexBuilder;
import com.github.onsdigital.zebedee.exceptions.ZebedeeException;
import com.github.onsdigital.zebedee.model.ContentWriter;
import com.github.onsdigital.zebedee.model.content.CompoundContentReader;
import com.github.onsdigital.zebedee.reader.ContentReader;
import com.github.onsdigital.zebedee.reader.FileSystemContentReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class TimeseriesDataRemover {

    public static void removeTimeseriesData(String[] args) throws Exception {

        // args[1] - source data directory
        // args[2] - destination directory to save the updated timeseries (can be a collection or master)
        // args[3] - the type of data to remove (monthly | quarterly | yearly)
        // args[4]... the timeseries CDID's to update

        Path source = Paths.get(args[1]);
        Path destination = Paths.get(args[2]);

        DataResoltion dataResoltion = DataResoltion.valueOf(args[3]);
        Set<String> cdids = new HashSet<>();

        for (int i = 4; i < args.length; i++) {
            cdids.add(args[i]);
            System.out.println("Adding CDID: " + args[i]);
        }

        removeTimeseriesData(source, destination, dataResoltion, cdids);
    }

    private static void removeTimeseriesData(Path source, Path destination, DataResoltion dataResoltion, Set<String> cdids) throws InterruptedException, ZebedeeException, IOException {

        // build the data index so we know where to find timeseries files given the CDID
        ContentReader contentReader = new FileSystemContentReader(source);

        // create a compound reader to check if the file already exists in the destination before reading it from the source.
        CompoundContentReader compoundContentReader = new CompoundContentReader(contentReader);
        compoundContentReader.add(new FileSystemContentReader(destination));

        ContentWriter contentWriter = new ContentWriter(destination);

        DataIndex dataIndex = DataIndexBuilder.buildDataIndex(contentReader);

        for (String cdid : cdids) {
            String uri = dataIndex.getUriForCdid(cdid.toLowerCase());

            if (uri == null) {
                System.out.println("CDID " + cdid + " not found in the data index.");
                continue;
            }

            TimeSeries page = (TimeSeries) compoundContentReader.getContent(uri);

            switch (dataResoltion) {
                case months:
                    System.out.println("Removing monthly data for CDID " + cdid + " with url: " + uri);
                    page.months = new TreeSet<>();
                    break;
                case quarters:
                    System.out.println("Removing quarterly data for CDID " + cdid + " with url: " + uri);
                    page.quarters = new TreeSet<>();
                    break;
                case years:
                    System.out.println("Removing yearly data for CDID " + cdid + " with url: " + uri);
                    page.years = new TreeSet<>();
                    break;
            }

            contentWriter.writeObject(page, uri + "/data.json");
        }
    }


    public enum DataResoltion {
        months,
        quarters,
        years
    }
}
