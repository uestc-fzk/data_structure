public class Solution {
    /**
     * 代码中的类名、方法名、参数名已经指定，请勿修改，直接返回方法规定的值即可
     * <p>
     * 计算当然新冠感染总人数
     *
     * @param days int整型 疫情传播天数
     * @return long长整型
     */
    public long countPatient(int days) {
        long[] arr = new long[15];
        arr[1] = 1;

        long result=1;
        while (--days > 0) {
            // 进入新的一天
            arr[14] += arr[13];
            arr[13] = 0L;
            for (int i = 13; i > 1; i--) {
                arr[i] = arr[i - 1];
            }
            // 今日感染
            long origin = 0L;
            for (int i = 7; i <= 13; i++) {
                origin += arr[i];
            }
            long newGan = origin * 3;// 新感染人数
            arr[1] = newGan;
            result+=newGan;
        }

        return result;
    }


    public static void main(String[] args) {
        System.out.println(new Solution().countPatient(70));
    }
}