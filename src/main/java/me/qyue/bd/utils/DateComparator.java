package me.qyue.bd.utils;

import me.qyue.bd.model.Data;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class DateComparator implements Comparator<Data> {

    @Override
    public int compare(Data o1, Data o2) {
        String[] first = o1.birthday().split("-");
        String[] second = o2.birthday().split("-");
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
