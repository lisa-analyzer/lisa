package it.unive.lisa.analysis.string;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class BricksTest {

    @Test
    public void normBricksTest() {
        List<Brick> list = new ArrayList<>();

        HashSet<String> strings = new HashSet<>();
        strings.add("a");

        HashSet<String> strings1 = new HashSet<>();
        strings1.add("a");
        strings1.add("b");

        list.add(new Brick(1, 1, strings));
        list.add(new Brick(2, 3, strings1));
        list.add(new Brick(0, 1, strings1));

        Bricks bricks = new Bricks(list);

        bricks.normBricks();

        List<Brick> resultList = new ArrayList<>();

        Collection<String> resultHashSet = new HashSet<>();
        resultHashSet.add("aaa");
        resultHashSet.add("aab");
        resultHashSet.add("aba");
        resultHashSet.add("abb");

        resultList.add(new Brick(1, 1, resultHashSet));
        resultList.add(new Brick(0, 2, strings1));

        assertEquals(bricks, new Bricks(resultList));
    }

    @Test
    public void normBricksRule1Test() {
        List<Brick> list = new ArrayList<>();

        HashSet<String> strings = new HashSet<>();
        strings.add("a");
        strings.add("b");

        list.add(new Brick(1, 1, strings));
        list.add(new Brick(0, 0, new HashSet<>()));

        Bricks bricks = new Bricks(list);

        list.remove(1);

        assertEquals(bricks, new Bricks(list));
    }

    @Test
    public void normBricksRule2Test() {
        List<Brick> list = new ArrayList<>();

        Collection<String> strings = new HashSet<>();
        strings.add("a");
        strings.add("cd");

        Collection<String> strings1 = new HashSet<>();

        strings1.add("b");
        strings1.add("ef");

        list.add(new Brick(1, 1, strings));
        list.add(new Brick(1, 1, strings1));

        Bricks bricks = new Bricks(list);
        bricks.normBricks();

        Collection<String> resultStrings = new HashSet<>();

        List<Brick> resultList = new ArrayList<>();

        resultStrings.add("ab");
        resultStrings.add("aef");
        resultStrings.add("cdb");
        resultStrings.add("cdef");

        resultList.add(new Brick(1, 1, resultStrings));

        assertEquals(bricks, new Bricks(resultList));
    }

    @Test
    public void normBricksRule3Test() {
        List<Brick> list = new ArrayList<>();

        Collection<String> strings = new HashSet<>();
        strings.add("a");
        strings.add("b");
        strings.add("c");

        list.add(new Brick(2, 2, strings));

        Bricks bricks = new Bricks(list);

        bricks.normBricks();

        Collection<String> resultStrings = new HashSet<>();

        List<Brick> resultList = new ArrayList<>();

        resultStrings.add("aa");
        resultStrings.add("bb");
        resultStrings.add("cc");
        resultStrings.add("ab");
        resultStrings.add("ac");
        resultStrings.add("ba");
        resultStrings.add("bc");
        resultStrings.add("ca");
        resultStrings.add("cb");

        resultList.add(new Brick(1, 1, resultStrings));

        assertEquals(bricks, new Bricks(resultList));
    }

    @Test
    public void normBricksRule4Test() {
        List<Brick> list = new ArrayList<>();

        HashSet<String> strings = new HashSet<>();
        strings.add("a");
        strings.add("b");

        HashSet<String> strings1 = new HashSet<>();
        strings1.add("a");
        strings1.add("b");

        list.add(new Brick(0, 1, strings));
        list.add(new Brick(0, 2, strings1));

        Bricks bricks = new Bricks(list);

        bricks.normBricks();

        List<Brick> resultList = new ArrayList<>();

        resultList.add(new Brick(0, 3, strings));

        assertEquals(bricks, new Bricks(resultList));
    }

    @Test
    public void normBricksRule5Test() {
        List<Brick> list = new ArrayList<>();

        HashSet<String> strings = new HashSet<>();
        strings.add("a");

        list.add(new Brick(2, 5, strings));

        Bricks bricks = new Bricks(list);

        bricks.normBricks();

        List<Brick> resultList = new ArrayList<>();

        HashSet<String> resultStrings = new HashSet<>();
        resultStrings.add("aa");

        HashSet<String> resultStrings1 = new HashSet<>();
        resultStrings1.add("a");

        resultList.add(new Brick(1, 1, resultStrings));

        resultList.add(new Brick(0, 3, resultStrings1));

        assertEquals(bricks, new Bricks(resultList));
    }

    @Test
    public void testPadList() {
        HashSet<String> strings0 = new HashSet<>();
        strings0.add("a");

        Brick b0 = new Brick(2, 5, strings0);

        HashSet<String> strings1 = new HashSet<>();
        strings1.add("b");

        Brick b1 = new Brick(1, 3, strings1);


        HashSet<String> strings2 = new HashSet<>();
        strings2.add("c");

        Brick b2 = new Brick(0, 2, strings2);

        HashSet<String> strings3 = new HashSet<>();
        strings3.add("d");

        Brick b3 = new Brick(0, 1, strings3);

        HashSet<String> strings4 = new HashSet<>();
        strings4.add("e");

        Brick b4 = new Brick(2, 2, strings4);

        HashSet<String> strings5 = new HashSet<>();
        strings5.add("f");

        Brick b5 = new Brick(0, 2, strings5);

        List<Brick> bricksList1 = new ArrayList<>();
        List<Brick> bricksList2 = new ArrayList<>();

        bricksList1.add(b0);
        bricksList1.add(b1);
        bricksList1.add(b2);

        bricksList2.add(b3);
        bricksList2.add(b0);
        bricksList2.add(b1);
        bricksList2.add(b4);
        bricksList2.add(b5);

        Bricks bricks1 = new Bricks(bricksList1);
        Bricks bricks2 = new Bricks(bricksList2);

        List<Brick> resultList = new ArrayList<>();

        resultList.add(new Brick(0, 0, new HashSet<>()));
        resultList.add(b0);
        resultList.add(b1);
        resultList.add(new Brick(0, 0, new HashSet<>()));
        resultList.add(b2);

        System.out.println("bricks1: \n" + bricks1);
        System.out.println("bricks2: \n" + bricks2);
        assertEquals(bricks1.padList(bricks2), new Bricks(resultList));
    }
}
