package com.agentdid127.resourcepack.impl;

import com.agentdid127.resourcepack.Converter;
import com.agentdid127.resourcepack.PackConverter;
import com.agentdid127.resourcepack.Util;
import com.agentdid127.resourcepack.extra.PropertiesEx;
import com.agentdid127.resourcepack.pack.Pack;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;


public class MCPatcherConverter extends Converter {

    public MCPatcherConverter(PackConverter packConverter) {
        super(packConverter);
    }

    /**
     * Parent conversion for MCPatcher
     * @param pack
     * @throws IOException
     */
    @Override
    public void convert(Pack pack) throws IOException {
        Path models = pack.getWorkingPath().resolve("assets" + File.separator + "minecraft" +File.separator + "optifine");
        if (models.toFile().exists()) findFiles(models);
        //remapModelJson(models.resolve("item"));
        //remapModelJson(models.resolve("block"));
    }

    /**
     * Finds all files in Path path
     * @param path
     * @throws IOException
     */
    protected void findFiles(Path path) throws IOException {
        File directory = new File(path.toString());
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isDirectory()) {
                remapProperties(Paths.get(file.getPath()));
                findFiles(Paths.get(file.getPath()));

            }
        }
    }

    /**
     * Remaps properties to work in newer versions
     * @param path
     * @throws IOException
     */
    protected void remapProperties(Path path) throws IOException {

        if (!path.toFile().exists()) return;

        Files.list(path)
                .filter(path1 -> path1.toString().endsWith(".properties"))
                .forEach(model -> {
                    try (InputStream input = new FileInputStream( model.toString())) {
                        PropertiesEx prop = new PropertiesEx();
                        prop.load(input);

                        try(OutputStream output = new FileOutputStream( model.toString())) {
                            //updates textures
                            if (prop.containsKey("texture")) {
                                prop.setProperty("texture", replaceTextures(prop));
                            }
                            //Updates Item IDs
                            if (prop.containsKey("matchItems")) {
                                prop.setProperty("matchItems", updateID("matchItems", prop).replaceAll("\"", ""));

                            }
                            if (prop.containsKey("matchBlocks")) {
                                prop.setProperty("matchBlocks", updateID("matchBlocks", prop).replaceAll("\"", ""));

                            }



                            //Saves File
                            prop.store(output, "");
                        }

                        catch (IOException io) {
                            io.printStackTrace();
                        }


                    } catch (IOException e) {
                        throw Util.propagate(e);
                    }
                });
    }

    /**
     * Replaces texture paths with blocks and items
     * @param prop
     * @return
     */
    protected String replaceTextures(PropertiesEx prop) {
        NameConverter nameConverter = packConverter.getConverter(NameConverter.class);
        String properties = prop.getProperty("texture");

        if (properties.startsWith("textures/blocks/")) {
            properties = "textures/block/" + nameConverter.getBlockMapping();
        } else if (properties.startsWith("textures/items/")) {
            properties = "textures/item/" + nameConverter.getItemMapping();
        }
        else{
            return properties;
        }
        return properties;
    }

    /**
     * Fixes item IDs and switches them from a numerical id to minecraft: something
     * @param type
     * @param prop
     * @return
     */
    protected String updateID(String type, PropertiesEx prop) {
        JsonObject id = Util.readJsonResource(packConverter.getGson(), "/ids.json");
        String properties2 = new String();
        for (Map.Entry<String, JsonElement> id2 : id.entrySet()) {
            if (prop.getProperty(type).contains(" ")) {
                String[] split = prop.getProperty(type).split(" ");
                for (int i = 0; i < split.length; i++) {
                        if (prop.containsKey("metadata")) {
                            if ((split[i] + ":" + prop.getProperty("metadata")).equals(id2.getKey())) {
                                properties2 = properties2 + " " + id2.getValue().getAsString();
                                System.out.println("renamed " + prop.getProperty(type) + " to " + properties2);
                            }
                        }

                    else if ((split[i]).equals(id2.getKey())) {
                        properties2 = properties2 + " " + id2.getValue().getAsString();
                        System.out.println("renamed " + prop.getProperty(type) + " to " + properties2);
                    }

                }


            } else {

                if (prop.getProperty(type).equals(id2.getKey())) {
                    String value = new String();
                        if (prop.containsKey("metadata")) {
                            value = id2.getValue().getAsString() + ":" + prop.getProperty("metadata");

                        } else {
                            value = id2.getValue().getAsString();
                        }

                    properties2 = value;
                    System.out.println("renamed " + prop.getProperty(type) + " to " + value);
                    return properties2;
                }
            }

            }
        if(prop.containsKey("metadata")) prop.remove("metadata");
        if (properties2 != "") {
            return properties2;

        } else {;
            return prop.getProperty(type);
        }


    }
}
