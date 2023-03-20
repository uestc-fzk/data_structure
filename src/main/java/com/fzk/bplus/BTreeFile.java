package com.fzk.bplus;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

/**
 * 文件B+树
 * 暂时只考虑插入和查询, 不考虑删除
 * 指针/pos: 文件内绝对定位
 * 页下标/pageIndex: 即文件内第几页, 默认第0页不使用, 从第1页开始使用
 * 页内偏移/off：页内偏移量, 一般 {页指针 + 页内偏移量} 才是文件内绝对定位
 *
 * @author fzk
 * @datetime 2023-03-20 23:23:55
 */
//@SuppressWarnings("unused")
public class BTreeFile {
    public static final int InvalidPosition = 0;// 将0作为非法指针值, 因为文件默认数据是0
    public static final int InvalidIndex = 0;// 将0作为非法指针值, 因为文件默认数据是0
    public static final int PageSize = 4 * 1024;// 默认一页4KB
    public static final int PageNum = 100;// 默认页面总数

    private final FileChannel fileChannel;
    private final MappedByteBuffer fileMap;

    public BTreeFile(Path path) throws IOException {
        // 文件初始化
        fileChannel = FileChannel.open(path, Set.of(StandardOpenOption.WRITE, StandardOpenOption.READ));
        fileMap = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, PageNum * PageSize);
        // 文件头初始化
        fileHeader = new FileHeader();
        fileHeader.rootPageIndex = 1;
        fileHeader.headPageIndex = 1;
        fileHeader.pageCount = 1;// 注意：默认第0页不使用
    }

    //---------- 文件头 -----------
    private final FileHeader fileHeader;

    //---------- 文件头 -----------
    public static class FileHeader {
        public static final int File_Header_Size = 4 + 4 + 4;// 文件头size
        public int rootPageIndex;// 根结点页面页下标
        public int headPageIndex;// 叶子结点链表头结点页面页下标
        public int pageCount;// 已使用页数量
    }

    // 一页就是一个结点, 暂时只考虑插入和查询, 不考虑删除
    public static class Page {
        public static final int IndexPage = 1;// 索引页
        public static final int LeafPage = 2;// 叶子页
        public static final int Key_Max_Num = 64;// 页内最多包含key数量

        public PageHeader pageHeader;// 页头
        public MappedByteBuffer fileMap;
        public FileChannel fileChannel;

        public int pagePos;// 此页指针
        public int pageIndex;// 此页面页下标

        // 字节数组方便计算长度
        public boolean addKey(byte[] key) {
            // 1.存储位置: 页指针+页内偏移
            int keyOff = pageHeader.writeIndex;
            int keyIdx = pageHeader.keyCount;
            int keyPos = pagePos + keyOff;
            // 2.检查
            // 2.1 key槽写满了
            if (pageHeader.keyCount >= Page.Key_Max_Num) {
                return false;
            }
            // 2.2 页面写满了
            if (pageHeader.writeIndex + 4 + key.length >= BTreeFile.PageSize) {
                return false;
            }
            // 3.写入页内
            fileMap.putInt(keyPos, key.length);// 数据长度
            fileMap.put(keyPos + 4, key);
            // 4.更新页头
            pageHeader.writeIndex += (4 + key.length);
            pageHeader.keyCount++;
            pageHeader.keyOffList[keyIdx] = keyOff;// 将key存储页内偏移写入key槽
            pageHeader.childPageIndexList[keyOff] = InvalidIndex;// 暂时不加子页面
            return true;
        }

        // ---- 页头 ----
        public static class PageHeader {
            public static final int Page_Header_Size = 4 * 4 + 4 * Page.Key_Max_Num + 4 * Page.Key_Max_Num;// 页头size
            public int pageType;// 页类型: 1为索引页, 2为叶子页
            public int parentPageIndex;// 父节点页下标
            public int keyCount;// key数量
            public int writeIndex;// 写索引，页内偏移量, 代表当前页内已使用数据的偏移量

            public int[] keyOffList;// key槽, key数据存储页内偏移
            public int[] childPageIndexList;// key指向子页面页下标
        }

        // 数据信息
        public static class KeyData {
            public int keyPos;// key数据存储绝对位置指针
            public int len;// 数据长度

            public byte[] data;

            public KeyData(int keyPos, int len, byte[] data) {
                this.keyPos = keyPos;
                this.len = len;
                this.data = data;
            }
        }
    }

}