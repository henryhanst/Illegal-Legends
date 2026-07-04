package ee.taltech.examplegame.server.TMXLoaders;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.BaseTmxMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.XmlReader.Element;

/** @brief synchronous loader for TMX maps created with the Tiled tool */
public class HijackedTmxLoader extends HijackedBaseTmxMapLoader<HijackedTmxLoader.Parameters> {

    public static class Parameters extends BaseTmxMapLoader.Parameters {

    }

    /** Creates loader
     *
     * @param resolver */
    public HijackedTmxLoader (FileHandleResolver resolver) {
        super(resolver);
    }

    /** Loads the {@link TiledMap} from the given file. The file is resolved via the {@link FileHandleResolver} set in the
     * constructor of this class. By default it will resolve to an internal file. The map will be loaded for a y-up coordinate
     * system.
     * @param fileName the filename
     * @return the TiledMap */
    public TiledMap load (String fileName) {
        return load(fileName, new Parameters());
    }

    /** Loads the {@link TiledMap} from the given file. The file is resolved via the {@link FileHandleResolver} set in the
     * constructor of this class. By default it will resolve to an internal file.
     * @param fileName the filename
     * @param parameter specifies whether to use y-up, generate mip maps etc.
     * @return the TiledMap */
    public TiledMap load (String fileName, Parameters parameter) {
        FileHandle tmxFile = resolve(fileName);

        this.root = xml.parse(tmxFile);

        TiledMap map = loadTiledMap(tmxFile, parameter);
        return map;
    }

    @Override
    public void loadAsync (AssetManager manager, String fileName, FileHandle tmxFile, Parameters parameter) {
        this.map = loadTiledMap(tmxFile, parameter);
    }

    @Override
    public TiledMap loadSync (AssetManager manager, String fileName, FileHandle file, Parameters parameter) {
        return map;
    }

    @Override
    protected Array<AssetDescriptor> getDependencyAssetDescriptors (FileHandle tmxFile,	TextureLoader.TextureParameter textureParameter) {
        Array<AssetDescriptor> descriptors = new Array<AssetDescriptor>();

        final Array<FileHandle> fileHandles = getDependencyFileHandles(tmxFile);
        for (FileHandle handle : fileHandles) {
            descriptors.add(new AssetDescriptor(handle, null, textureParameter));
        }

        return descriptors;
    }

    /**
     * Load file dependencies
     * @param tmxFile that is being loaded
     * @return array of filehandles
     */
    private Array<FileHandle> getDependencyFileHandles (FileHandle tmxFile) {
        Array<FileHandle> fileHandles = new Array<FileHandle>();

        // TileSet descriptors
        for (Element tileset : root.getChildrenByName("tileset")) {
            String source = tileset.getAttribute("source", null);
            if (source != null) {
                FileHandle tsxFile = getRelativeFileHandle(tmxFile, source);
                tileset = xml.parse(tsxFile);
                Element imageElement = tileset.getChildByName("image");
                if (imageElement != null) {
                    String imageSource = tileset.getChildByName("image").getAttribute("source");
                    FileHandle image = getRelativeFileHandle(tsxFile, imageSource);
                    fileHandles.add(image);
                } else {
                    for (Element tile : tileset.getChildrenByName("tile")) {
                        String imageSource = tile.getChildByName("image").getAttribute("source");
                        FileHandle image = getRelativeFileHandle(tsxFile, imageSource);
                        fileHandles.add(image);
                    }
                }
            } else {
                Element imageElement = tileset.getChildByName("image");
                if (imageElement != null) {
                    String imageSource = tileset.getChildByName("image").getAttribute("source");
                    FileHandle image = getRelativeFileHandle(tmxFile, imageSource);
                    fileHandles.add(image);
                } else {
                    for (Element tile : tileset.getChildrenByName("tile")) {
                        String imageSource = tile.getChildByName("image").getAttribute("source");
                        FileHandle image = getRelativeFileHandle(tmxFile, imageSource);
                        fileHandles.add(image);
                    }
                }
            }
        }

        // ImageLayer descriptors
        for (Element imageLayer : root.getChildrenByName("imagelayer")) {
            Element image = imageLayer.getChildByName("image");
            String source = image.getAttribute("source", null);

            if (source != null) {
                FileHandle handle = getRelativeFileHandle(tmxFile, source);
                fileHandles.add(handle);
            }
        }

        return fileHandles;
    }

    /**
     * Adds tiles that are in the whole map
     */
    @Override
    protected void addStaticTiles (FileHandle tmxFile, TiledMapTileSet tileSet, Element element,
                                   Array<Element> tileElements, String name, int firstgid, int tilewidth, int tileheight, int spacing, int margin,
                                   String source, int offsetX, int offsetY, String imageSource, int imageWidth, int imageHeight, FileHandle image) {

        MapProperties props = tileSet.getProperties();
        if (image != null) {

            props.put("imagesource", imageSource);
            props.put("imagewidth", imageWidth);
            props.put("imageheight", imageHeight);
            props.put("tilewidth", tilewidth);
            props.put("tileheight", tileheight);
            props.put("margin", margin);
            props.put("spacing", spacing);

            int id = firstgid;

        } else {
            // Every tile has its own image source
            for (Element tileElement : tileElements) {
                Element imageElement = tileElement.getChildByName("image");
                if (imageElement != null) {
                    imageSource = imageElement.getAttribute("source");

                    if (source != null) {
                        image = getRelativeFileHandle(getRelativeFileHandle(tmxFile, source), imageSource);
                    } else {
                        image = getRelativeFileHandle(tmxFile, imageSource);
                    }
                }
                int tileId = firstgid + tileElement.getIntAttribute("id");
                addStaticTiledMapTile(tileSet, tileId, offsetX, offsetY);
            }
        }
    }
}
