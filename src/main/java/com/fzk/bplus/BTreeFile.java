package com.fzk.bplus;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

/**
 * 文件B+树
 *
 * @author fzk
 * @datetime 2023-03-20 23:23:55
 */
public class BTreeFile {
    public static final int InvalidPosition = 0;// 将0作为非法指针值, 因为文件默认数据是0
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
        fileHeader.rootNodePosition = FileHeader.FileHeaderSize;
        fileHeader.headNodePosition = FileHeader.FileHeaderSize;
        fileHeader.nodeCount = 0;
    }

    //---------- 文件头 -----------
    private final FileHeader fileHeader;

    //---------- 文件头 -----------
    public static class FileHeader {
        public long rootNodePosition;// 根结点指针
        public long headNodePosition;// 叶子结点链表头结点指针
        public int nodeCount;
        public static final long FileHeaderSize = 8 + 8 + 4;// 文件头size
    }

    // 一页就是一个结点
    public static class PageNode {
        public long position;// 此节点指针
        public long parentNode;// 父节点指针

        public int keyCount;// key数量
        public long[] keys;// key数据指针
        public long[] childList;// 子结点指针

    }

    // 数据信息
    public static class KeyData {
        public long position;// 数据位置指针
        public int len;// 数据长度

        public String key;
    }
}