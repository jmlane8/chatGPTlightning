import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import java.lang.Math;
import java.awt.Color;

import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

class Main {
    static double t_base = 4000.;
    static List<Map<String, Object>> std_atm = new ArrayList<Map<String, Object>>(Arrays.asList(
            new HashMap<String, Object>() {{
                put("height", 0.);
                put("pressure", 101325.);
                put("temperature", 15.);
                put("sat_vp", 17.1);
            }},
            new HashMap<String, Object>() {{
                put("height", 1000.);
                put("pressure", 89880.);
                put("temperature", 8.5);
                put("sat_vp", 11.1);
            }},
            new HashMap<String, Object>() {{
                put("height", 2000.);
                put("pressure", 79500.);
                put("temperature", 2.);
                put("sat_vp", 7.1);
            }},
            new HashMap<String, Object>() {{
                put("height", 3000.);
                put("pressure", 70012.);
                put("temperature", -4.49);
                put("sat_vp", 0.);
            }},
            new HashMap<String, Object>() {{
                put("height", 4000.);
                put("pressure", 61660.);
                put("temperature", -10.98);
                put("sat_vp", 0.);
            }}
    ));
    static List<Map<String, Object>> atm_w_density = new ArrayList<Map<String, Object>>();
    static List<Double> atm_height_list = new ArrayList<Double>();
    static List<Map<String, Object>> graph_list = new ArrayList<Map<String, Object>>();

    static void run() throws IOException {
        atm_w_density = build_atm_w_density(std_atm);
        atm_height_list = build_atm_height_list(std_atm);
        int start_i = get_start_idx(t_base, atm_height_list);
        for (int andx = 0; andx < 22; andx++) {
            double s_angle = 4 * andx;
            double angle = s_angle;
            double curr_y = t_base;
            double curr_x = 0;
            int indx = start_i;
            List<Double> x_list = new ArrayList<Double>();
            List<Double> y_list = new ArrayList<Double>();
            x_list.add(0.0);
            y_list.add(4000.0);

            int jndx = 0;
            while (jndx < 20 && curr_y > 0 && !(curr_y >= 4000 && jndx > 0)) {
                jndx++;
                int next_i;
                if (angle < 90) {
                    next_i = indx - 1;
                } else if (angle > 90) {
                    next_i = indx + 1;
                } else {
                    next_i = indx;
                }
                double next_y;
                try {
                    next_y = atm_height_list.get(next_i);
                } catch (IndexOutOfBoundsException e) {
                    if (next_i == 0) {
                        jndx = 20;
                        next_y = 4000;
                    } else {
                        jndx = 20;
                        next_y = 0;
                    }
                }
            double next_x = get_next_x(curr_x, curr_y, angle, next_y);
            x_list.add(next_x);
            y_list.add(next_y);
            double next_angle = get_next_angle(angle, indx, atm_w_density, false, next_i);
            curr_x = next_x;
            curr_y = next_y;
            indx = next_i;
            angle = next_angle;
        }
        Map<String, Object> graph_item = new HashMap<String, Object>();
        graph_item.put("s_angle", s_angle);
        graph_item.put("x_list", x_list);
        graph_item.put("y_list", y_list);
        graph_list.add(graph_item);
    }
    print_graph_list(graph_list);
    XYSeriesCollection dataset = new XYSeriesCollection();
for (Map<String, Object> graph_item : graph_list) {
        double  s_angle = (double) graph_item.get("s_angle");
        XYSeries series = new XYSeries(s_angle);
        List<Double> x_list = (List<Double>) graph_item.get("x_list");
        List<Double> y_list = (List<Double>) graph_item.get("y_list");
        for (int i = 0; i < x_list.size(); i++) {
            series.add(x_list.get(i), y_list.get(i));
        }
        dataset.addSeries(series);
    }
    JFreeChart chart = ChartFactory.createXYLineChart("Propagation of Thunder in Standard Atmosphere", "Horizontal Distance (m)", "Height (m)", dataset, PlotOrientation.VERTICAL, true, true, false);
chart.getXYPlot().setBackgroundPaint(Color.WHITE);
        ValueAxis xAxis = chart.getXYPlot().getDomainAxis();
        xAxis.setRange(0, 25000); // sets the x-axis range from 0 to 100
    ChartFrame frame = new ChartFrame("Thunder Propagation", chart);
        int width = 640;
        int height = 480;
        File file = new File("chart.png");
        ChartUtils.saveChartAsPNG(file, chart, width, height);
frame.pack();
frame.setVisible(true);
}

    static int get_start_idx(double height, List<Double> std_atm_height_list) {
        return std_atm_height_list.indexOf(height);
    }

    static double get_next_x(double curr_x,
                             double curr_y, double angle, double next_y) {
        double rangle = Math.toRadians(angle);
        double delta_y = curr_y - next_y;
        double delta_x = delta_y * Math.tan(rangle);
        return curr_x + delta_x;
    }

    static double get_next_angle(double angle, int indx, List<Map<String, Object>> atm_w_density, boolean is_water, int next_i) {
        Map<String, Object> atm_parameters1 = atm_w_density.get(indx);
        Map<String, Object> atm_parameters2 = atm_w_density.get(next_i);
        String key;
        key = (is_water) ? "humid_sp_sound" : "dry_sp_sound";

        double v1 = (double) atm_parameters1.get(key);
        double v2 = (double) atm_parameters2.get(key);
        int dens_ndx;
        double new_angle;
        //double humidity_ratio = (double) ((humid_density / 28.9644) / (dry_density / 28.9644 - humid_density / 28.9644));
        //double pressure = (double) atm_parameters.get("pressure");
        //double humidity = (double) (humidity_ratio / (1 + humidity_ratio));
        //double pressure_in_atm = pressure / 101325;
        //double es = (double) (6.11 * Math.pow(10, (7.5 * temperature) / (237.7 + temperature)));
        //double e = (double) (humidity * pressure / (0.378 * humidity + 0.622)); //specific humidity
        https://earthscience.stackexchange.com/questions/2360/how-do-i-convert-specific-humidity-to-relative-humidity
        //double c = (double) (20.05 * Math.sqrt(e / pressure_in_atm));
        //speed of sound equal to 20.05 times sqrt temp in K
        // https://www.engineeringtoolbox.com/air-speed-sound-d_603.html
        //double a = (double) (c * c / sp_sound);
        new_angle  = getRefraction(angle, v1, v2);
        if (new_angle < 0) {
            new_angle += 360;
        } else if (new_angle > 360) {
            new_angle -= 360;
        }
        return new_angle;
    }
    public static double getRefraction(double dangle1, double v1, double v2) {
        double rangle1 = Math.toRadians(dangle1);
        double expr = Math.sin(rangle1) * v2 / v1;
        if (expr == 1) {
            return 90;
        } else if (expr > 1) {
            expr = 2 - expr;
            double rangle2 = Math.asin(expr);
            double dangle2 = Math.toDegrees(rangle2);
            return 180 - dangle2;
        } else {
            double rangle2 = Math.asin(expr);
            return Math.toDegrees(rangle2);
        }
    }
    static List<Map<String, Object>> build_atm_w_density(List<Map<String, Object>> std_atm) {
        List<Map<String, Object>> atm_w_density = new ArrayList<Map<String, Object>>();
        for (int i = 0; i < std_atm.size(); i++) {
            Map<String, Object> atm_parameters = new HashMap<String, Object>();
            Map<String, Object> std_atm_parameters = std_atm.get(i);
            double height = (double) std_atm_parameters.get("height");
            double pressure = (double) std_atm_parameters.get("pressure");
            double temperature = (double) std_atm_parameters.get("temperature");
            double sat_vp = (double) std_atm_parameters.get("sat_vp");
            double dry_density = (double) (pressure / (287.058 * (temperature + 273.15)));
            double humid_density = (double) (pressure / (287.058 * (temperature + 273.15) - sat_vp));

            double dry_sp_sound = getSpeedSound(pressure, dry_density);
            double humid_sp_sound = getSpeedSound(pressure, humid_density);
            atm_parameters.put("height", height);
            atm_parameters.put("pressure", pressure);
            atm_parameters.put("temperature", temperature);
            atm_parameters.put("sat_vp", sat_vp);
            atm_parameters.put("dry_density", dry_density);
            atm_parameters.put("humid_density", humid_density);
            atm_parameters.put("dry_sp_sound", dry_sp_sound);
            atm_parameters.put("humid_sp_sound", humid_sp_sound);
            atm_w_density.add(atm_parameters);
        }
        return atm_w_density;
    }

    private static double getSpeedSound(double pressure, double density) {
        double kappa = 1.402;
        double sound = Math.sqrt(kappa * pressure / density);
        return sound;
    }

    static List<Double> build_atm_height_list(List<Map<String, Object>> std_atm) {
        List<Double> atm_height_list = new ArrayList<Double>();
        for (Map<String, Object> std_atm_parameters : std_atm) {
            atm_height_list.add((double) std_atm_parameters.get("height"));
        }
        return atm_height_list;
    }

    static void print_graph_list(List<Map<String, Object>> graph_list) {
        for (Map<String, Object> graph_item : graph_list) {
            System.out.println(graph_item.get("s_angle"));
            List<Double> x_list = (List<Double>) graph_item.get("x_list");
            List<Double> y_list = (List<Double>) graph_item.get("y_list");
            for (int i = 0; i < x_list.size(); i++) {
                System.out.println(x_list.get(i) + ", " + y_list.get(i));
            }
            System.out.println();
        }
    }

    public static void main(String[] args) throws IOException {
        run();
    }
}
