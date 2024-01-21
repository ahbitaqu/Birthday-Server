package me.qyue.bd.utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class DateComparator implements Comparator<HashMap<String, String>> {

    @Override
    public int compare(HashMap<String, String> o1, HashMap<String, String> o2) {
        String[] first = o1.values().toArray(new String[0]);
        first = first[0].split("-");
        String[] second = o2.values().toArray(new String[0]);
        second = second[0].split("-");
        //comparing if the month is equal
        if (Objects.equals(Integer.parseInt(first[1]), Integer.parseInt(second[1]))) {
            //comparing if day is equal
            if (Integer.parseInt(first[0]) == Integer.parseInt(second[0])) return 0;
            return Integer.parseInt(first[0]) < Integer.parseInt(second[0]) ? -1 : 1;
        } else {
            return Integer.parseInt(first[1]) < Integer.parseInt(second[1]) ? -1 : 1;
        }
    }
}
