package org.litteraworlds.map;

import org.litteraworlds.utils.HashGen;
import org.litteraworlds.objects.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Region implements Serializable {

    private String regionHashID;

    private byte[] regionHashIDBytes;

    private String regionName;

    private ArrayList<Zone> zones = new ArrayList<>();

    public Region(){
        this("unknownRegion",new byte[0]);
    }

    public Region(String regionName, byte[] regionHashIDBytes){
        this.regionName = regionName;

        this.regionHashIDBytes = HashGen.getHash(regionHashIDBytes);

        this.regionHashID = convertHashToString(this.regionHashIDBytes);
    }

    private static String convertHashToString(byte[] hash){
        String temp = "";
        for(byte b : hash){
            temp = temp.concat(String.format("%02x",b));
        }
        return temp;
    }

    public List<Zone> getZones() {
        return zones;
    }

    public String getRegionHashID() {
        return regionHashID;
    }

    public void putPlayerIntoRandomZone(Player player){
        int zoneNum = new Random().nextInt(zones.size());
        player.putIntoMap(zones.get(zoneNum));
    }

    public byte[] getRegionHashIDBytes() {
        return regionHashIDBytes;
    }

    public String getRegionName() {
        return regionName;
    }

    public void putZone(Zone zone){
        this.zones.add(zone);
    }

    public Zone getZone(String name){
        return this.zones.stream().filter(zone -> zone.getPlaceName().equals(name)).findFirst().orElseThrow();
    }

    public Zone getZone(byte[] hash){
        return this.zones.stream().filter(zone -> Arrays.equals(zone.getPlaceHashIDBytes(), hash)).findFirst().orElseThrow();
    }

    @Override
    public String toString() {
        return "Region:" + this.regionName;
    }
}
