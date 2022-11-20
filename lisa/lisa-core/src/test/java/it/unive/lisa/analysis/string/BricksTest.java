package it.unive.lisa.analysis.string;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BricksTest {

    @Test
    public void normBricksTest(){
        List<Brick> list = new ArrayList<>();

        HashSet<String> hashSet = new HashSet<>();
        hashSet.add("a");

        HashSet<String> hashSet1 = new HashSet<>();
        hashSet1.add("a");
        hashSet1.add("b");

        list.add(new Brick(1,1,hashSet));
        list.add(new Brick(2,3,hashSet1));
        list.add(new Brick(0,1,hashSet1));

        Bricks bricks = new Bricks(list);

        bricks.normBricks();

        List<Brick> resultList = new ArrayList<>();

        HashSet<String> resultHashSet = new HashSet<>();
        resultHashSet.add("aaa");
        resultHashSet.add("aab");
        resultHashSet.add("aba");
        resultHashSet.add("abb");

        resultList.add(new Brick(1,1,resultHashSet));
        resultList.add(new Brick(0,2,hashSet1));

        Bricks resultBricks = new Bricks(resultList);

        assertEquals(bricks,resultBricks);
    }
}
