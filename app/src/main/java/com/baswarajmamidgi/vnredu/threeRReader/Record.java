package com.baswarajmamidgi.vnredu.threeRReader;

import java.util.Date;

public class Record {
    private static int nextId = 0;
    String label;
    Date dateTime;
    String pathToImage;
    int id = ++nextId;
}
