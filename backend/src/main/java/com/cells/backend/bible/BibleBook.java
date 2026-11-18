package com.cells.backend.bible;

/** The 66 books, in canonical order, with their chapter counts. */
public enum BibleBook {
    GENESIS("Genesis", 50),
    EXODUS("Exodus", 40),
    LEVITICUS("Leviticus", 27),
    NUMBERS("Numbers", 36),
    DEUTERONOMY("Deuteronomy", 34),
    JOSHUA("Joshua", 24),
    JUDGES("Judges", 21),
    RUTH("Ruth", 4),
    FIRST_SAMUEL("1 Samuel", 31),
    SECOND_SAMUEL("2 Samuel", 24),
    FIRST_KINGS("1 Kings", 22),
    SECOND_KINGS("2 Kings", 25),
    FIRST_CHRONICLES("1 Chronicles", 29),
    SECOND_CHRONICLES("2 Chronicles", 36),
    EZRA("Ezra", 10),
    NEHEMIAH("Nehemiah", 13),
    ESTHER("Esther", 10),
    JOB("Job", 42),
    PSALMS("Psalms", 150),
    PROVERBS("Proverbs", 31),
    ECCLESIASTES("Ecclesiastes", 12),
    SONG_OF_SOLOMON("Song of Solomon", 8),
    ISAIAH("Isaiah", 66),
    JEREMIAH("Jeremiah", 52),
    LAMENTATIONS("Lamentations", 5),
    EZEKIEL("Ezekiel", 48),
    DANIEL("Daniel", 12),
    HOSEA("Hosea", 14),
    JOEL("Joel", 3),
    AMOS("Amos", 9),
    OBADIAH("Obadiah", 1),
    JONAH("Jonah", 4),
    MICAH("Micah", 7),
    NAHUM("Nahum", 3),
    HABAKKUK("Habakkuk", 3),
    ZEPHANIAH("Zephaniah", 3),
    HAGGAI("Haggai", 2),
    ZECHARIAH("Zechariah", 14),
    MALACHI("Malachi", 4),
    MATTHEW("Matthew", 28),
    MARK("Mark", 16),
    LUKE("Luke", 24),
    JOHN("John", 21),
    ACTS("Acts", 28),
    ROMANS("Romans", 16),
    FIRST_CORINTHIANS("1 Corinthians", 16),
    SECOND_CORINTHIANS("2 Corinthians", 13),
    GALATIANS("Galatians", 6),
    EPHESIANS("Ephesians", 6),
    PHILIPPIANS("Philippians", 4),
    COLOSSIANS("Colossians", 4),
    FIRST_THESSALONIANS("1 Thessalonians", 5),
    SECOND_THESSALONIANS("2 Thessalonians", 3),
    FIRST_TIMOTHY("1 Timothy", 6),
    SECOND_TIMOTHY("2 Timothy", 4),
    TITUS("Titus", 3),
    PHILEMON("Philemon", 1),
    HEBREWS("Hebrews", 13),
    JAMES("James", 5),
    FIRST_PETER("1 Peter", 5),
    SECOND_PETER("2 Peter", 3),
    FIRST_JOHN("1 John", 5),
    SECOND_JOHN("2 John", 1),
    THIRD_JOHN("3 John", 1),
    JUDE("Jude", 1),
    REVELATION("Revelation", 22);

    public enum Testament { OLD, NEW }

    private static final int OLD_TESTAMENT_BOOKS = 39;

    private final String displayName;
    private final int chapters;

    BibleBook(String displayName, int chapters) {
        this.displayName = displayName;
        this.chapters = chapters;
    }

    public String displayName() { return displayName; }
    public int chapters() { return chapters; }

    public Testament testament() {
        return ordinal() < OLD_TESTAMENT_BOOKS ? Testament.OLD : Testament.NEW;
    }

    /** "John 3", "John 3:16" or "John 3:16–18" (en dash written as an escape, so the file encoding never matters). */
    public String reference(int chapter, Integer verseStart, Integer verseEnd) {
        if (verseStart == null) {
            return displayName + " " + chapter;
        }
        if (verseEnd == null) {
            return displayName + " " + chapter + ":" + verseStart;
        }
        return displayName + " " + chapter + ":" + verseStart + "\u2013" + verseEnd;
    }
}