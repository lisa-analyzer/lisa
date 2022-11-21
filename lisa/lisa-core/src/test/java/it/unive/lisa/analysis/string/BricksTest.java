package it.unive.lisa.analysis.string;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BricksTest {

    @Test
    public void normBricksTest(){
        List<Brick> list = new ArrayList<>();

        HashSet<String> strings = new HashSet<>();
        strings.add("a");

        HashSet<String> strings1 = new HashSet<>();
        strings1.add("a");
        strings1.add("b");

        list.add(new Brick(1,1,strings));
        list.add(new Brick(2,3,strings1));
        list.add(new Brick(0,1,strings1));

        Bricks bricks = new Bricks(list);

        bricks.normBricks();

        List<Brick> resultList = new ArrayList<>();

        Collection<String> resultHashSet = new HashSet<>();
        resultHashSet.add("aaa");
        resultHashSet.add("aab");
        resultHashSet.add("aba");
        resultHashSet.add("abb");

        resultList.add(new Brick(1,1,resultHashSet));
        resultList.add(new Brick(0,2,strings1));

        Bricks resultBricks = new Bricks(resultList);

        assertEquals(bricks,resultBricks);
    }

    @Test
    public void normBricksRule1Test(){
        List<Brick> list = new ArrayList<>();

        HashSet<String> strings = new HashSet<>();
        strings.add("a");
        strings.add("b");

        list.add(new Brick(1,1,strings));
        list.add(new Brick(0,0,new HashSet<>()));

        Bricks bricks = new Bricks(list);

        list.remove(1);

        Bricks resultBricks = new Bricks(list);

        assertEquals(bricks,resultBricks);
    }

    @Test
    public void normBricksRule2Test(){
        List<Brick> list = new ArrayList<>();

        Collection<String> strings = new HashSet<>();
        strings.add("a");
        strings.add("cd");

        Collection<String> strings1 = new HashSet<>();

        strings1.add("b");
        strings1.add("ef");

        list.add(new Brick(1,1,strings));
        list.add(new Brick(1,1,strings1));

        Bricks bricks = new Bricks(list);
        bricks.normBricks();

        Collection<String> resultStrings = new HashSet<>();

        List<Brick> resultList = new ArrayList<>();


        resultStrings.add("ab");
        resultStrings.add("aef");
        resultStrings.add("cdb");
        resultStrings.add("cdef");

        resultList.add(new Brick(1,1,resultStrings));

        Bricks resultBricks = new Bricks(resultList);

        assertEquals(bricks,resultBricks);
    }
}
