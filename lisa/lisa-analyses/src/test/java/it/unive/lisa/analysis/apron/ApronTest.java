package it.unive.lisa.analysis.apron;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ApronTest {

    @BeforeAll
    public static void setupLibrary() {
        if (Apron.isAvailable()) {
            return;
        }

        // Expected absolute path
        String libPath = "insert/absolute/path";

        // /Users/username/Library/Java/Extensions/
        Apron.loadLibrary();

        // Custom path
        // Apron.loadLibrary(libPath);
    }

    @Test
    public void testApronBoxInitialization() {
        assumeTrue(Apron.isAvailable(), "Apron native library not found. Box test not executed.");

        try {
            // Test for Box domain
            Apron.setManager(Apron.ApronDomain.Box);

            Apron state = new Apron().top();

            assertTrue(state.isTop(), "Created state is TOP");
            System.out.println("Success: JApron loaded and Box created");

        } catch (UnsupportedOperationException e) {
            e.printStackTrace(System.err);
            fail("Error: JApron crashed or library is not really available: " + e.getMessage());
        } catch (Exception | Error e) {
            e.printStackTrace(System.err);
            fail("Fatal Error during JNI call: " + e.getMessage());
        }
    }

    @Test
    public void testApronPolkaInitialization() {
        assumeTrue(Apron.isAvailable(), "Apron native library not found. Polka test not executed.");

        try {
            // Test for Polka domain
            Apron.setManager(Apron.ApronDomain.Polka);

            Apron state = new Apron().bottom();

            assertTrue(state.isBottom(), "Created state is BOTTOM");
            System.out.println("Success: JApron loaded and Polka created");

        } catch (UnsupportedOperationException e) {
            e.printStackTrace(System.err);
            fail("Error: JApron crashed or library is not really available: " + e.getMessage());
        } catch (Exception | Error e) {
            e.printStackTrace(System.err);
            fail("Fatal Error during JNI call: " + e.getMessage());
        }
    }
}