package com.test.spg.my_ux.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RoutePlan {
    public static List<myPoint> getAllPoints(myPoint... points) {
        double lat_min = points[0].lat;
        double lat_max = points[0].lat;
        double lng_min = points[0].lng;
        double lng_max = points[0].lng;

        for (myPoint point : points) {
            if (lat_min > point.lat) lat_min = point.lat;
            if (lat_max < point.lat) lat_max = point.lat;
            if (lng_min > point.lng) lng_min = point.lng;
            if (lng_max < point.lng) lng_max = point.lng;
        }

        // 定位左下的点
        double temp_lat = lat_max;
        for (myPoint point : points) {
            if (point.lng == lng_min && point.lat < temp_lat) temp_lat = point.lat;
        }

        // 相对左下的点为起始点
        myPoint p0 = new myPoint(lng_min, temp_lat);
        myPoint p1 = null;

        // 每变化10米，经纬度变化的度数，这里需要优化，不准确
        double dt_lng = 0.0001169619;
        double dt_lat = 0.0000898093;

        // 最终所有的坐标点存储
        List<myPoint> out_points = new ArrayList<>();
        out_points.add(p0);

        // 交点存储
        List<myPoint> nodes = new ArrayList<>();
        myPoint temp_point;

        for (double temp_lng = lng_min + dt_lng; temp_lng < lng_max; temp_lng += dt_lng) {
            //System.out.println(temp_lng);
            // 获取交点
            nodes.clear();
            for (int i = 0; i < points.length - 1; i++) {
                temp_point = getIntersectionVertical(new myPoint(lat_min, temp_lng), points[i], points[i + 1]);
                if ((temp_point.lat < points[i].lat) != (temp_point.lat < points[i + 1].lat) && !temp_point.isEmpty()) {
                    nodes.add(temp_point);
                }
            }
            temp_point = getIntersectionVertical(new myPoint(lat_min, temp_lng), points[points.length - 1], points[0]);
            if ((temp_point.lat < points[points.length - 1].lat) != (temp_point.lat < points[0].lat) && !temp_point.isEmpty()) {
                nodes.add(temp_point);
            }

            double node_lat_max = nodes.get(0).lat;
            double node_lat_min = nodes.get(0).lat;
            for (myPoint node : nodes) {
                if (node_lat_max < node.lat) node_lat_max = node.lat;
                if (node_lat_min > node.lat) node_lat_min = node.lat;
            }
            out_points.add(nodes.get(0));
            for (temp_lat = node_lat_min + dt_lat; temp_lat < node_lat_max; temp_lat += dt_lat) {
                temp_point = new myPoint(temp_lat, temp_lng);
                out_points.add(temp_point);
            }

            // 太近了就去掉前一个点
            if (nodes.get(nodes.size()-1).lat - out_points.get(out_points.size()-1).lat  < dt_lat/2){
                out_points.remove(out_points.size()-1);
            }
            out_points.add(nodes.get(nodes.size() - 1));
        }
        out_points.remove(0);
        out_points = rearrange((ArrayList<myPoint>) out_points);
        return  out_points;
    }

    public static myPoint getIntersectionVertical(myPoint loopPoint, myPoint p1, myPoint p2) {
        myPoint node = new myPoint(0, 0);
        if (p1.lng == p2.lng) {
            // 平行了
            return new myPoint(0, 0);
        } else {
            node.lng = loopPoint.lng;
            double k = (p2.lat - p1.lat) / (p2.lng - p1.lng);
            node.lat = k * (node.lng - p1.lng) + p1.lat;
            return node;
        }
    }

    /**
     * 判断是否在多边形内部
     *
     * @param testP  判断点
     * @param points 多边形顶点
     * @return
     */
    public static boolean isInPoly(myPoint testP, myPoint... points) {
        boolean isOdd = false;
        for (int i = 0; i < points.length; i++) {
            for (int j = i + 1; j < points.length; j++) {
                if (((testP.lat < points[i].lat) != (testP.lat < points[j].lat))
                        && (testP.lng <= (points[j].lng - points[i].lng) * (testP.lat - points[i].lat) / (points[j].lat - points[i].lat) + points[i].lng))
                    isOdd = !isOdd;
            }
        }
        return isOdd;
    }


    public static List<myPoint> rearrange(ArrayList<myPoint> out_points){
        boolean isOdd = false;
        myPoint temp = out_points.get(0);
        ArrayList<myPoint> outList = new ArrayList<>();
        ArrayList<myPoint> columnList = new ArrayList<>();

        for (myPoint point : out_points) {
            if (point.lng == temp.lng) {
                columnList.add(point);
            }else if (isOdd){
                Collections.reverse(columnList);
                temp = point;
                outList.addAll(columnList);
                columnList.clear();
                columnList.add(point);
                isOdd = false;
            }else {
                temp = point;
                outList.addAll(columnList);
                columnList.clear();
                columnList.add(point);
                isOdd = true;
            }
        }
        return outList;
    }
}
