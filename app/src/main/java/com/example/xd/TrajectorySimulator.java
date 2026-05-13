package com.example.xd;

import java.util.ArrayList;
import java.util.List;

public class TrajectorySimulator {
    private List<TrajectoryPoint> points;
    private int currentIndex;
    private boolean isRunning;
    private float totalDistance;

    public static class TrajectoryPoint {
        public double latitude;
        public double longitude;
        public double altitude;
        public float bearing;
        public float speed;
        public float accuracy;

        public TrajectoryPoint(double lat, double lon) {
            this.latitude = lat;
            this.longitude = lon;
            this.altitude = 0;
            this.bearing = 0;
            this.speed = 0;
            this.accuracy = 5.0f;
        }

        public TrajectoryPoint(double lat, double lon, double alt,
                              float bearing, float speed, float accuracy) {
            this.latitude = lat;
            this.longitude = lon;
            this.altitude = alt;
            this.bearing = bearing;
            this.speed = speed;
            this.accuracy = accuracy;
        }
    }

    public TrajectorySimulator() {
        points = new ArrayList<>();
        currentIndex = 0;
        isRunning = false;
        totalDistance = 0;
    }

    public void addPoint(double lat, double lon) {
        points.add(new TrajectoryPoint(lat, lon));
    }

    public void addPoint(double lat, double lon, double alt,
                        float bearing, float speed, float accuracy) {
        points.add(new TrajectoryPoint(lat, lon, alt, bearing, speed, accuracy));
    }

    public void addPoint(TrajectoryPoint point) {
        points.add(point);
    }

    public void clearPoints() {
        points.clear();
        currentIndex = 0;
        totalDistance = 0;
    }

    public TrajectoryPoint getNextPoint() {
        if (points.isEmpty()) {
            return null;
        }
        if (currentIndex >= points.size()) {
            currentIndex = 0;
        }
        return points.get(currentIndex++);
    }

    public TrajectoryPoint getCurrentPoint() {
        if (points.isEmpty() || currentIndex >= points.size()) {
            return null;
        }
        return points.get(currentIndex);
    }

    public boolean hasMorePoints() {
        return !points.isEmpty() && currentIndex < points.size();
    }

    public void reset() {
        currentIndex = 0;
    }

    public void start() {
        isRunning = true;
        currentIndex = 0;
    }

    public void stop() {
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getPointCount() {
        return points.size();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int index) {
        if (index >= 0 && index < points.size()) {
            currentIndex = index;
        }
    }

    public List<TrajectoryPoint> getPoints() {
        return new ArrayList<>(points);
    }

    public void generateCirclePath(double centerLat, double centerLon,
                                   double radiusMeters, int numPoints) {
        clearPoints();
        double earthRadius = 6371000;
        double latRad = Math.toRadians(centerLat);
        double lonRad = Math.toRadians(centerLon);

        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            double dx = radiusMeters * Math.cos(angle);
            double dy = radiusMeters * Math.sin(angle);

            double newLatRad = latRad + (dy / earthRadius);
            double newLonRad = lonRad + (dx / (earthRadius * Math.cos(latRad)));

            double bearing = (float) Math.toDegrees(angle);
            TrajectoryPoint point = new TrajectoryPoint();
            point.latitude = Math.toDegrees(newLatRad);
            point.longitude = Math.toDegrees(newLonRad);
            point.altitude = 0;
            point.bearing = bearing;
            point.speed = 5.0f;
            point.accuracy = 5.0f;
            addPoint(point);
        }
    }

    public void generateLinePath(double startLat, double startLon,
                                 double endLat, double endLon, int numPoints) {
        clearPoints();
        for (int i = 0; i < numPoints; i++) {
            double ratio = (double) i / (numPoints - 1);
            double lat = startLat + (endLat - startLat) * ratio;
            double lon = startLon + (endLon - startLon) * ratio;
            addPoint(new TrajectoryPoint(lat, lon));
        }
    }

    public void generateRectanglePath(double centerLat, double centerLon,
                                     double widthMeters, double heightMeters, int pointsPerSide) {
        clearPoints();
        double halfWidth = widthMeters / 2;
        double halfHeight = heightMeters / 2;

        double lat1 = centerLat + metersToDegreesLat(halfHeight);
        double lon1 = centerLon - metersToDegreesLon(halfWidth, centerLat);
        double lat2 = centerLat + metersToDegreesLat(halfHeight);
        double lon2 = centerLon + metersToDegreesLon(halfWidth, centerLat);
        double lat3 = centerLat - metersToDegreesLat(halfHeight);
        double lon3 = centerLon + metersToDegreesLon(halfWidth, centerLat);
        double lat4 = centerLat - metersToDegreesLat(halfHeight);
        double lon4 = centerLon - metersToDegreesLon(halfWidth, centerLat);

        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / pointsPerSide;
            addPoint(new TrajectoryPoint(
                lat1 + (lat2 - lat1) * t,
                lon1 + (lon2 - lon1) * t
            ));
        }
        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / pointsPerSide;
            addPoint(new TrajectoryPoint(
                lat2 + (lat3 - lat2) * t,
                lon2 + (lon3 - lon2) * t
            ));
        }
        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / pointsPerSide;
            addPoint(new TrajectoryPoint(
                lat3 + (lat4 - lat3) * t,
                lon3 + (lon4 - lon3) * t
            ));
        }
        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / pointsPerSide;
            addPoint(new TrajectoryPoint(
                lat4 + (lat1 - lat4) * t,
                lon4 + (lon1 - lon4) * t
            ));
        }
    }

    private double metersToDegreesLat(double meters) {
        return meters / 111320.0;
    }

    private double metersToDegreesLon(double meters, double latitude) {
        return meters / (111320.0 * Math.cos(Math.toRadians(latitude)));
    }

    public double calculateTotalDistance() {
        if (points.size() < 2) return 0;
        totalDistance = 0;
        for (int i = 0; i < points.size() - 1; i++) {
            totalDistance += calculateDistance(points.get(i), points.get(i + 1));
        }
        return totalDistance;
    }

    public float calculateDistance(TrajectoryPoint p1, TrajectoryPoint p2) {
        double earthRadius = 6371000;
        double lat1Rad = Math.toRadians(p1.latitude);
        double lat2Rad = Math.toRadians(p2.latitude);
        double deltaLat = Math.toRadians(p2.latitude - p1.latitude);
        double deltaLon = Math.toRadians(p2.longitude - p1.longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (earthRadius * c);
    }

    public TrajectoryPoint interpolate(TrajectoryPoint p1, TrajectoryPoint p2, float ratio) {
        TrajectoryPoint result = new TrajectoryPoint();
        result.latitude = p1.latitude + (p2.latitude - p1.latitude) * ratio;
        result.longitude = p1.longitude + (p2.longitude - p1.longitude) * ratio;
        result.altitude = p1.altitude + (p2.altitude - p1.altitude) * ratio;
        result.bearing = p1.bearing + (p2.bearing - p1.bearing) * ratio;
        result.speed = p1.speed + (p2.speed - p1.speed) * ratio;
        result.accuracy = p1.accuracy + (p2.accuracy - p1.accuracy) * ratio;
        return result;
    }
}
