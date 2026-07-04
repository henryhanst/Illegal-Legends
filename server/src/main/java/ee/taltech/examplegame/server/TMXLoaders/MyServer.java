package ee.taltech.examplegame.server.TMXLoaders;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;

/**
 * THIS IS AN EXAMPLE CLASS
 */
public class MyServer {


    /**
     * Modified filehandle that is built on libGDX's filehandler.
     */
    public static class MyFileHandleResolver implements FileHandleResolver {
        @Override
        public FileHandle resolve(String fileName) {
            System.out.println("Trying to resolve map: " + fileName);
            // Resolve file using Java's File class
            File file = new File(fileName);
            return new FileHandle(file); // Use FileHandle directly
        }
    }


}
