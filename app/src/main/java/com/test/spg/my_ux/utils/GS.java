package com.test.spg.my_ux.utils;

public class GS {
    //椭球基本参数
    public static double a = 6378137;//椭球长半轴
    public static double b = 6356752.31414;//椭球短半轴
    public static double f, e2, e_prime2;//椭球扁率、第一偏心率的平方、第二偏心率的平方
    public static double B1, L1, A1=0, S = 10;//起点纬度、经度、大地方位角、大地线长度
    public static double B2, L2, A2;//终点纬度、经度、大地方位角

    public static double Deg2Rad(double deg)
    {
        double rad = deg * Math.PI / 180.0;
        return rad;
    }
    public static double Rad2Deg(double rad)
    {
        double deg = rad * 180.0 / Math.PI;
        return deg;
    }
    static void calculation(double B1,double L1,double A1,double S) {
        B1 = Deg2Rad(B1);
        L1 = Deg2Rad(L1);
        A1 = Deg2Rad(A1);

        f = (a - b) / a;
        e2 = (a * a - b * b) / (a * a);
        e_prime2 = e2 / (1 - e2);

        //计算起点归化纬度

        double W1 = Math.pow(1 - e2 * Math.sin(B1) * Math.sin(B1), 0.5);
        double sin_mu1 = Math.sin(B1) * Math.sqrt(1 - e2) / W1;
        double cos_mu1 = Math.cos(B1) / W1;

        //计算辅助函数值
        double sin_A0 = cos_mu1 * Math.sin(A1);
        double cot_delta1 = cos_mu1 * Math.cos(A1) / sin_mu1;
        double delta1 = Math.atan2(1, cot_delta1);

        //计算系数A B C及αβγ
        double cos_A02 = 1 - sin_A0 * sin_A0;
        double k2 = e_prime2 * cos_A02;
        double A = (1 - k2 / 4 + 7 * k2 * k2 / 64 - 15 * Math.pow(k2, 3) / 256) / b;
        double B = (k2 / 4 - k2 * k2 / 8 + 37 * Math.pow(k2, 3) / 512);
        double C = k2 * k2 / 128 - Math.pow(k2, 3) / 128;
        double alpha = (e2 / 2 + e2 * e2 / 8 + e2 * e2 * e2 / 16) - (e2 * e2 / 16 + e2 * e2 * e2 / 16) * cos_A02 + (3 * e2 * e2 * e2 / 128) * cos_A02 * cos_A02;
        double beta = (e2 * e2 / 16 + e2 * e2 * e2 / 16) * cos_A02 - (e2 / 32) * cos_A02 * cos_A02;
        double gamma = (e2 * e2 * e2 / 256) * cos_A02 * cos_A02;

        //计算球面长度
        double delta = A * S;
        double temp = 0;
        do {
            double delta_temp = delta;
            delta = A * S + B * Math.sin(delta) * Math.cos(2 * delta1 + delta) + C * Math.sin(2 * delta) * Math.cos(4 * delta1 + 2 * delta);
            temp = delta - delta_temp;

        } while (Math.abs(temp) < 1e-10);

        //计算经度差改正数
        //其中Delta=Lambda-L
        double Delta = (alpha * delta + beta * Math.sin(delta) * Math.cos(2 * delta1 + delta) + gamma * Math.sin(2 * delta) * Math.cos(4 * delta1 + 2 * delta)) * sin_A0;

        //计算终点大地坐标及坐标方位角
        double sin_mu2 = sin_mu1 * Math.cos(delta) + cos_mu1 * Math.cos(A1) * Math.sin(delta);
        B2 = Math.atan2(sin_mu2, (Math.sqrt(1 - e2) * Math.sqrt(1 - sin_mu2 * sin_mu2)));
        double Lambda = Math.atan2(Math.sin(A1) * Math.sin(delta), (cos_mu1 * Math.cos(delta) - sin_mu1 * Math.sin(delta) * Math.cos(A1)));
        if (Math.sin(A1) > 0) {
            if (Math.tan(Lambda) > 0) {
                Lambda = Math.abs(Lambda);
            } else {
                Lambda = Math.PI - Math.abs(Lambda);
            }
        } else {
            if (Math.tan(Lambda) < 0) {
                Lambda = -Math.abs(Lambda);
            } else {
                Lambda = Math.abs(Lambda) - Math.PI;
            }
        }
        L2 = L1 + Lambda - Delta;
        B2 = Rad2Deg(B2);
        L2 = Rad2Deg(L2);
    }


    //计算经度差
    public  static double cal_DeltaLng(double B1, double L1, double S)
    {
        B1 = Deg2Rad(B1);
        L1 = Deg2Rad(L1);
        double A1 = Deg2Rad(90);

        f = (a - b) / a;
        e2 = (a * a - b * b) / (a * a);
        e_prime2 = e2 / (1 - e2);

        //计算起点归化纬度
        double W1 = Math.pow(1 - e2 * Math.sin(B1) * Math.sin(B1), 0.5);
        double sin_mu1 = Math.sin(B1) * Math.sqrt(1 - e2) / W1;
        double cos_mu1 = Math.cos(B1) / W1;

        //计算辅助函数值
        double sin_A0 = cos_mu1 * Math.sin(A1);
        double cot_delta1 = cos_mu1 * Math.cos(A1) / sin_mu1;
        double delta1 = Math.atan2(1, cot_delta1);

        //计算系数A B C及αβγ
        double cos_A02 = 1 - sin_A0 * sin_A0;
        double k2 = e_prime2 * cos_A02;
        double A = (1 - k2 / 4 + 7 * k2 * k2 / 64 - 15 * Math.pow(k2, 3) / 256) / b;
        double B = (k2 / 4 - k2 * k2 / 8 + 37 * Math.pow(k2, 3) / 512);
        double C = k2 * k2 / 128 - Math.pow(k2, 3) / 128;
        double alpha = (e2 / 2 + e2 * e2 / 8 + e2 * e2 * e2 / 16) - (e2 * e2 / 16 + e2 * e2 * e2 / 16) * cos_A02 + (3 * e2 * e2 * e2 / 128) * cos_A02 * cos_A02;
        double beta = (e2 * e2 / 16 + e2 * e2 * e2 / 16) * cos_A02 - (e2 / 32) * cos_A02 * cos_A02;
        double gamma = (e2 * e2 * e2 / 256) * cos_A02 * cos_A02;

        //计算球面长度
        double delta = A * S;
        double temp = 0;
        do
        {
            double delta_temp = delta;
            delta = A * S + B * Math.sin(delta) * Math.cos(2 * delta1 + delta) + C * Math.sin(2 * delta) * Math.cos(4 * delta1 + 2 * delta);
            temp = delta - delta_temp;

        } while (Math.abs(temp) < 1e-10);

        //计算经度差改正数
        //其中Delta=Lambda-L
        double Delta = (alpha * delta + beta * Math.sin(delta) * Math.cos(2 * delta1 + delta) + gamma * Math.sin(2 * delta) * Math.cos(4 * delta1 + 2 * delta)) * sin_A0;

        //计算终点大地坐标及坐标方位角
        double sin_mu2 = sin_mu1 * Math.cos(delta) + cos_mu1 * Math.cos(A1) * Math.sin(delta);
        B2 = Math.atan2(sin_mu2, (Math.sqrt(1 - e2) * Math.sqrt(1 - sin_mu2 * sin_mu2)));
        double Lambda = Math.atan2(Math.sin(A1) * Math.sin(delta), (cos_mu1 * Math.cos(delta) - sin_mu1 * Math.sin(delta) * Math.cos(A1)));
        if (Math.sin(A1) > 0)
        {
            if (Math.tan(Lambda) > 0)
            {
                Lambda = Math.abs(Lambda);
            }
            else
            {
                Lambda = Math.PI - Math.abs(Lambda);
            }
        }
        else
        {
            if (Math.tan(Lambda) < 0)
            {
                Lambda = -Math.abs(Lambda);
            }
            else
            {
                Lambda = Math.abs(Lambda) - Math.PI;
            }
        }
        L2 = L1 + Lambda - Delta;
        double DeltaLng = Rad2Deg(Lambda - Delta);
        return DeltaLng;
    }

    //计算纬度差
    public static double cal_DeltaLat(double B1, double L1, double S)
    {
        B1 = Deg2Rad(B1);
        L1 = Deg2Rad(L1);
        A1 = Deg2Rad(A1);

        f = (a - b) / a;
        e2 = (a * a - b * b) / (a * a);
        e_prime2 = e2 / (1 - e2);

        //计算起点归化纬度
        double W1 = Math.pow(1 - e2 * Math.sin(B1) * Math.sin(B1), 0.5);
        double sin_mu1 = Math.sin(B1) * Math.sqrt(1 - e2) / W1;
        double cos_mu1 = Math.cos(B1) / W1;

        //计算辅助函数值
        double sin_A0 = cos_mu1 * Math.sin(A1);
        double cot_delta1 = cos_mu1 * Math.cos(A1) / sin_mu1;
        double delta1 = Math.atan2(1, cot_delta1);

        //计算系数A B C及αβγ
        double cos_A02 = 1 - sin_A0 * sin_A0;
        double k2 = e_prime2 * cos_A02;
        double A = (1 - k2 / 4 + 7 * k2 * k2 / 64 - 15 * Math.pow(k2, 3) / 256) / b;
        double B = (k2 / 4 - k2 * k2 / 8 + 37 * Math.pow(k2, 3) / 512);
        double C = k2 * k2 / 128 - Math.pow(k2, 3) / 128;
        double alpha = (e2 / 2 + e2 * e2 / 8 + e2 * e2 * e2 / 16) - (e2 * e2 / 16 + e2 * e2 * e2 / 16) * cos_A02 + (3 * e2 * e2 * e2 / 128) * cos_A02 * cos_A02;
        double beta = (e2 * e2 / 16 + e2 * e2 * e2 / 16) * cos_A02 - (e2 / 32) * cos_A02 * cos_A02;
        double gamma = (e2 * e2 * e2 / 256) * cos_A02 * cos_A02;

        //计算球面长度
        double delta = A * S;
        double temp = 0;
        do
        {
            double delta_temp = delta;
            delta = A * S + B * Math.sin(delta) * Math.cos(2 * delta1 + delta) + C * Math.sin(2 * delta) * Math.cos(4 * delta1 + 2 * delta);
            temp = delta - delta_temp;

        } while (Math.abs(temp) < 1e-10);

        //计算经度差改正数
        //其中Delta=Lambda-L
        double Delta = (alpha * delta + beta * Math.sin(delta) * Math.cos(2 * delta1 + delta) + gamma * Math.sin(2 * delta) * Math.cos(4 * delta1 + 2 * delta)) * sin_A0;

        //计算终点大地坐标及坐标方位角
        double sin_mu2 = sin_mu1 * Math.cos(delta) + cos_mu1 * Math.cos(A1) * Math.sin(delta);
        B2 = Math.atan2(sin_mu2, (Math.sqrt(1 - e2) * Math.sqrt(1 - sin_mu2 * sin_mu2)));
        double DeltaLat = Rad2Deg(B2 - B1);
        return DeltaLat;
    }

    //计算纬线方向宽度
    public static double cal_Width(double H,double SO)//H 航高（飞机相位中心到地面的距离），SO 旁向重叠度（Side Overlap、百分数）
    {
        double h = 0.03;//飞机相位中心到胶片的距离,应内部设置
        double width = 0.02113;//胶片宽度，即垂直于航向宽度,应内部设置
        double Width;//取景宽度，即实地垂直于航向宽度,纬线方向

        //将相片大小放大到实地
        Width = width * (H+h) / h;

        //考虑重叠度，计算点间距
        Width = Width * (1 - SO);

        return Width;
    }

    //计算经线方向宽度
    public static double cal_Length(double H, double LO)//H 航高（飞机相位中心到地面的距离），LO 航向重叠度（Longitudinal Overlap、百分数）
    {
        double h = 0.01;//飞机相位中心到胶片的距离,应内部设置
        double length = 0.01408;//胶片长度，即沿航向长度,应内部设置
        double Length;//取景长度，即实地沿航向长度，经线方向

        //将相片大小放大到实地
        Length = length * (H +h)/ h;

        //考虑重叠度，计算点间距
        Length = Length * (1 - LO);

        return Length;
    }



    public static void main(String[] args) {
        calculation(30.3, 84.2, 95.5959, 78611.881);
        System.out.println("B2 = " + B2);
        System.out.println("L2 = " + L2);
        System.out.println("A2 = " + A2);
    }
}
