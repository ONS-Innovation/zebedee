package com.github.onsdigital.zebedee.util;

import au.com.bytecode.opencsv.CSVWriter;
import com.github.onsdigital.content.page.statistics.document.article.Article;
import com.github.onsdigital.content.page.statistics.document.bulletin.Bulletin;
import com.github.onsdigital.content.util.ContentUtil;
import com.github.onsdigital.zebedee.Zebedee;
import com.github.onsdigital.zebedee.api.Root;

import javax.swing.text.DateFormatter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by thomasridd on 06/07/15.
 */
public class Validator {
    Zebedee zebedee;

    public Validator(Zebedee zebedee) {
        this.zebedee = zebedee;
    }

    public void validate(Path path) throws IOException {

        Path bulletins = csvOfBulletinData();
        Files.copy(bulletins, path.resolve("bulletins.csv"));

        Path articles = csvOfArticleData();
        Files.copy(articles, path.resolve("articles.csv"));
    }

    public Path csvOfArticleData() throws IOException {
        Path path = Files.createTempFile("articles", ".csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(Files.newOutputStream(path), Charset.forName("UTF8")), ',')) {

            String[] row;
            row = new String[10];
            row[0] = "Theme";
            row[1] = "Level2";
            row[2] = "Level3";
            row[3] = "Title";
            row[4] = "URI Date";
            row[5] = "ReleaseDate";
            row[6] = "Title";
            row[7] = "Edition";
            row[8] = "Next Release";

            writer.writeNext(row);
            List<Bulletin> bulletins = bulletinList();

            List<Path> paths = filesMatching(articleMatcher());

            for (Path articlePath: paths) {
                Article article;
                try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(articlePath))) {
                    article = ContentUtil.deserialise(inputStream, Article.class);
                }

                row[0] = articlePath.subpath(1,2).toString();;
                row[1] = articlePath.subpath(2,3).toString();;
                if(articlePath.subpath(3,4).toString().equalsIgnoreCase("articles")) {
                    row[2] = "";
                    row[3] = articlePath.subpath(4,5).toString();
                    row[4] = articlePath.subpath(5,6).toString();
                } else {
                    row[2] = articlePath.subpath(3,4).toString();;
                    row[3] = articlePath.subpath(5,6).toString();;
                    row[4] = articlePath.subpath(6,7).toString();;
                }
                if (article.getDescription().getReleaseDate() != null) {
                    row[5] = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(article.getDescription().getReleaseDate());
                } else {
                    row[5] = "";
                }
                row[6] = article.getDescription().getTitle();
                row[7] = article.getDescription().getEdition();
                row[8] = article.getDescription().getNextRelease();
                writer.writeNext(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public Path csvOfBulletinData() throws IOException {
        Path path = Files.createTempFile("bulletins", ".csv");
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(Files.newOutputStream(path), Charset.forName("UTF8")), ',')) {

            String[] row;
            row = new String[10];
            row[0] = "Theme";
            row[1] = "Level2";
            row[2] = "Level3";
            row[3] = "Title";
            row[4] = "URI Date";
            row[5] = "ReleaseDate";
            row[6] = "Title";
            row[7] = "Edition";
            row[8] = "Next Release";
            writer.writeNext(row);
            List<Bulletin> bulletins = bulletinList();

            List<Path> paths = filesMatching(bulletinMatcher());

            for (Path bulletinPath: paths) {
                Bulletin bulletin;
                try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(bulletinPath))) {
                    bulletin = ContentUtil.deserialise(inputStream, Bulletin.class);
                }

                row[0] = bulletinPath.subpath(1,2).toString();;
                row[1] = bulletinPath.subpath(2,3).toString();;
                if(bulletinPath.subpath(3,4).toString().equalsIgnoreCase("bulletins")) {
                    row[2] = "";
                    row[3] = bulletinPath.subpath(4,5).toString();
                    row[4] = bulletinPath.subpath(5,6).toString();
                } else {
                    row[2] = bulletinPath.subpath(3,4).toString();;
                    row[3] = bulletinPath.subpath(5,6).toString();;
                    row[4] = bulletinPath.subpath(6,7).toString();;
                }
                if (bulletin.getDescription().getReleaseDate() != null) {
                    row[5] = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(bulletin.getDescription().getReleaseDate());
                } else {
                    row[5] = "";
                }
                row[6] = bulletin.getDescription().getTitle();
                row[7] = bulletin.getDescription().getEdition();
                row[8] = bulletin.getDescription().getNextRelease();
                writer.writeNext(row);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public List<Path> filesMatching(final PathMatcher matcher) throws IOException {
        Path startPath = zebedee.published.path;
        final List<Path> paths = new ArrayList<>();

        Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
            {
                if (matcher.matches(file)) {
                    paths.add(zebedee.path.relativize(file));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    public List<Bulletin> bulletinList() throws IOException {
        List<Path> paths = filesMatching(bulletinMatcher());

        List<Bulletin> bulletins = new ArrayList<>();
        for (Path path: paths) {
            try(InputStream inputStream = Files.newInputStream(zebedee.path.resolve(path))) {
                bulletins.add(ContentUtil.deserialise(inputStream, Bulletin.class));
            }
        }
        return bulletins;
    }

    public static PathMatcher bulletinMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().contains("data.json") && path.toString().contains("bulletins")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }

    public static PathMatcher articleMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().contains("data.json") && path.toString().contains("articles")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }

    public static PathMatcher timeSeriesMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().contains("data.json") && path.toString().contains("timeseries")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }

    public static PathMatcher dataSetMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().contains("data.json") && path.toString().contains("datasets")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }

    public static PathMatcher productPageMatcher() {
        PathMatcher matcher = new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                if (path.toString().contains("data.json") && path.toString().contains("bulletins")) {
                    return true;
                }
                return false;
            }
        };
        return  matcher;
    }

    public static void main(String[] args) {
        Validator validator = new Validator(Root.zebedee);
    }
}