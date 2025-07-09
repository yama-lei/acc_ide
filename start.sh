#!/bin/bash
# 创建IDE专用迷你rootfs - 完整脚本
# 此脚本在Ubuntu/Debian系统上运行

set -e  # 任何命令失败就退出

echo "===== 开始创建IDE专用迷你rootfs ====="

# 1. 安装必要工具
echo "安装必要工具..."
sudo apt update
sudo apt install -y debootstrap qemu-user-static xz-utils wget tar build-essential

# 2. 准备工作目录
echo "准备工作目录..."
WORK_DIR="$HOME/ide-rootfs"
mkdir -p "$WORK_DIR"
cd "$WORK_DIR"

# 3. 下载Alpine基础系统 (同时准备arm64和x86_64两个架构)
echo "下载Alpine基础系统..."

# ARM64版本
if [ ! -f alpine-minirootfs-3.18.0-aarch64.tar.gz ]; then
    wget https://dl-cdn.alpinelinux.org/alpine/v3.18/releases/aarch64/alpine-minirootfs-3.18.0-aarch64.tar.gz
fi

# 可选：x86_64版本
if [ ! -f alpine-minirootfs-3.18.0-x86_64.tar.gz ]; then
    wget https://dl-cdn.alpinelinux.org/alpine/v3.18/releases/x86_64/alpine-minirootfs-3.18.0-x86_64.tar.gz
fi

# 为两种架构创建目录
mkdir -p rootfs-arm64
mkdir -p rootfs-x86_64

# 4. ARM64版本的rootfs创建
echo "创建ARM64版本的rootfs..."
cd "$WORK_DIR/rootfs-arm64"
tar xzf ../alpine-minirootfs-3.18.0-aarch64.tar.gz

# 5. 准备chroot环境
echo "准备ARM64 chroot环境..."
sudo cp /usr/bin/qemu-aarch64-static usr/bin/
sudo cp /etc/resolv.conf etc/

# 6. 进入chroot环境并配置系统
echo "配置ARM64系统..."
sudo chroot . /usr/bin/qemu-aarch64-static /bin/sh << 'CHROOT_COMMANDS'
# 设置镜像源
echo "https://mirrors.tuna.tsinghua.edu.cn/alpine/v3.18/main" > /etc/apk/repositories
echo "https://mirrors.tuna.tsinghua.edu.cn/alpine/v3.18/community" >> /etc/apk/repositories

# 更新系统
apk update
apk upgrade

# 安装C/C++编译环境（精简版）
apk add --no-scripts gcc g++ make cmake musl-dev libc-dev binutils file patch

# 安装必要的运行库和工具
apk add libstdc++ libgcc ncurses-libs busybox bash coreutils

# 安装超时控制工具
apk add coreutils-timeout

# 清理不必要的包和缓存
rm -rf /var/cache/apk/*
rm -rf /usr/share/man/*
rm -rf /usr/share/doc/*
rm -rf /usr/lib/cmake/*
rm -rf /tmp/*

# 创建编译脚本
cat > /usr/local/bin/compile-cpp << 'EOF'
#!/bin/sh
CPP_FILE="$1"
OUTPUT="$2"
INPUT="$3"
OUTPUT_FILE="$4"
ERROR_FILE="$5"
TIMEOUT=2
MEM_LIMIT=512

# 编译
g++ -std=c++17 -Wall -O2 "$CPP_FILE" -o "$OUTPUT" 2> "$ERROR_FILE"

# 检查编译结果
if [ $? -ne 0 ]; then
  echo "编译错误" > "$ERROR_FILE"
  exit 1
fi

# 设置内存限制
ulimit -v $(($MEM_LIMIT * 1024))

# 运行带超时限制
timeout $TIMEOUT $OUTPUT < "$INPUT" > "$OUTPUT_FILE" 2>> "$ERROR_FILE"
EXIT_CODE=$?

# 检查运行结果
if [ $EXIT_CODE -eq 124 ]; then
  echo "TLE: 时间超限" > "$ERROR_FILE"
  exit 2
elif [ $EXIT_CODE -ne 0 ]; then
  echo "RE: 运行错误，返回码 $EXIT_CODE" > "$ERROR_FILE"
  exit 3
fi

exit 0
EOF

chmod +x /usr/local/bin/compile-cpp

# 添加一个环境检查脚本
cat > /usr/local/bin/check-env << 'EOF'
#!/bin/sh
echo "C/C++ IDE Runtime Environment"
echo "Alpine Linux $(cat /etc/alpine-release)"
echo "GCC版本: $(gcc --version | head -n1)"
echo "内存限制: 512MB"
echo "时间限制: 2秒"
echo "环境就绪"
EOF

chmod +x /usr/local/bin/check-env

# 删除大型开发文件
find /usr -name "*.a" -delete
find /usr -name "*.la" -delete
find /usr/lib -name "*.o" -delete

# 设置一个简单的环境变量配置
cat > /etc/profile.d/ide-env.sh << 'EOF'
export PATH=/usr/local/bin:$PATH
export LANG=C.UTF-8
EOF

# strip可执行文件减小体积
find /bin /usr/bin /sbin /usr/sbin -type f -executable -exec strip --strip-all {} \; 2>/dev/null || true
CHROOT_COMMANDS

# 7. 退出chroot环境后清理
echo "清理ARM64环境..."
sudo rm usr/bin/qemu-aarch64-static

# 8. 打包ARM64 rootfs
echo "打包ARM64 rootfs..."
cd "$WORK_DIR"
tar -c -I 'xz -9' -f alpine-ide-rootfs-arm64.tar.xz -C rootfs-arm64 .

# 9. 显示ARM64文件大小
du -h alpine-ide-rootfs-arm64.tar.xz

# 10. 可选：创建x86_64版本
if [ -f alpine-minirootfs-3.18.0-x86_64.tar.gz ]; then
    echo "创建x86_64版本的rootfs..."
    cd "$WORK_DIR/rootfs-x86_64"
    tar xzf ../alpine-minirootfs-3.18.0-x86_64.tar.gz

    # 准备x86_64 chroot环境
    echo "准备x86_64 chroot环境..."
    sudo cp /usr/bin/qemu-x86_64-static usr/bin/
    sudo cp /etc/resolv.conf etc/

    # 配置x86_64系统（与ARM64相同的配置）
    echo "配置x86_64系统..."
    sudo chroot . /usr/bin/qemu-x86_64-static /bin/sh << 'CHROOT_COMMANDS'
# 设置镜像源
echo "https://mirrors.tuna.tsinghua.edu.cn/alpine/v3.18/main" > /etc/apk/repositories
echo "https://mirrors.tuna.tsinghua.edu.cn/alpine/v3.18/community" >> /etc/apk/repositories

# 更新系统
apk update
apk upgrade

# 安装C/C++编译环境（精简版）
apk add --no-scripts gcc g++ make cmake musl-dev libc-dev binutils file patch

# 安装必要的运行库和工具
apk add libstdc++ libgcc ncurses-libs busybox bash coreutils

# 安装超时控制工具
apk add coreutils-timeout

# 清理不必要的包和缓存
rm -rf /var/cache/apk/*
rm -rf /usr/share/man/*
rm -rf /usr/share/doc/*
rm -rf /usr/lib/cmake/*
rm -rf /tmp/*

# 创建编译脚本
cat > /usr/local/bin/compile-cpp << 'EOF'
#!/bin/sh
CPP_FILE="$1"
OUTPUT="$2"
INPUT="$3"
OUTPUT_FILE="$4"
ERROR_FILE="$5"
TIMEOUT=2
MEM_LIMIT=512

# 编译
g++ -std=c++17 -Wall -O2 "$CPP_FILE" -o "$OUTPUT" 2> "$ERROR_FILE"

# 检查编译结果
if [ $? -ne 0 ]; then
  echo "编译错误" > "$ERROR_FILE"
  exit 1
fi

# 设置内存限制
ulimit -v $(($MEM_LIMIT * 1024))

# 运行带超时限制
timeout $TIMEOUT $OUTPUT < "$INPUT" > "$OUTPUT_FILE" 2>> "$ERROR_FILE"
EXIT_CODE=$?

# 检查运行结果
if [ $EXIT_CODE -eq 124 ]; then
  echo "TLE: 时间超限" > "$ERROR_FILE"
  exit 2
elif [ $EXIT_CODE -ne 0 ]; then
  echo "RE: 运行错误，返回码 $EXIT_CODE" > "$ERROR_FILE"
  exit 3
fi

exit 0
EOF

chmod +x /usr/local/bin/compile-cpp

# 添加一个环境检查脚本
cat > /usr/local/bin/check-env << 'EOF'
#!/bin/sh
echo "C/C++ IDE Runtime Environment"
echo "Alpine Linux $(cat /etc/alpine-release)"
echo "GCC版本: $(gcc --version | head -n1)"
echo "内存限制: 512MB"
echo "时间限制: 2秒"
echo "环境就绪"
EOF

chmod +x /usr/local/bin/check-env

# 删除大型开发文件
find /usr -name "*.a" -delete
find /usr -name "*.la" -delete
find /usr/lib -name "*.o" -delete

# 设置一个简单的环境变量配置
cat > /etc/profile.d/ide-env.sh << 'EOF'
export PATH=/usr/local/bin:$PATH
export LANG=C.UTF-8
EOF

# strip可执行文件减小体积
find /bin /usr/bin /sbin /usr/sbin -type f -executable -exec strip --strip-all {} \; 2>/dev/null || true
CHROOT_COMMANDS

    # 清理x86_64环境
    echo "清理x86_64环境..."
    sudo rm usr/bin/qemu-x86_64-static

    # 打包x86_64 rootfs
    echo "打包x86_64 rootfs..."
    cd "$WORK_DIR"
    tar -c -I 'xz -9' -f alpine-ide-rootfs-x86_64.tar.xz -C rootfs-x86_64 .

    # 显示x86_64文件大小
    du -h alpine-ide-rootfs-x86_64.tar.xz
fi

# 11. 下载proot二进制文件
echo "下载proot二进制文件..."
mkdir -p "$WORK_DIR/proot-bin"
cd "$WORK_DIR/proot-bin"

# 下载ARM64版本的proot
wget https://github.com/termux/proot-distro/raw/master/proot-static/proot-aarch64 -O proot-aarch64
chmod +x proot-aarch64

# 下载x86_64版本的proot
wget https://github.com/termux/proot-distro/raw/master/proot-static/proot-x86_64 -O proot-x86_64
chmod +x proot-x86_64

# 12. 创建最终目录结构
echo "创建最终目录结构..."
mkdir -p "$WORK_DIR/final/assets"

# 复制rootfs文件
cp "$WORK_DIR/alpine-ide-rootfs-arm64.tar.xz" "$WORK_DIR/final/assets/"
if [ -f "$WORK_DIR/alpine-ide-rootfs-x86_64.tar.xz" ]; then
    cp "$WORK_DIR/alpine-ide-rootfs-x86_64.tar.xz" "$WORK_DIR/final/assets/"
fi

# 复制proot二进制文件
cp "$WORK_DIR/proot-bin/proot-aarch64" "$WORK_DIR/final/assets/"
cp "$WORK_DIR/proot-bin/proot-x86_64" "$WORK_DIR/final/assets/"

# 13. 生成README文件
cat > "$WORK_DIR/final/README.txt" << 'EOF'
IDE迷你rootfs使用说明
=====================

目录结构:
- assets/alpine-ide-rootfs-arm64.tar.xz: ARM64架构的rootfs
- assets/alpine-ide-rootfs-x86_64.tar.xz: x86_64架构的rootfs
- assets/proot-aarch64: ARM64架构的proot二进制文件
- assets/proot-x86_64: x86_64架构的proot二进制文件

使用方法:
1. 将assets目录中的所有文件复制到Android项目的app/src/main/assets目录
2. 使用Java代码解压rootfs并执行proot
3. 通过proot执行compile-cpp脚本编译和运行C++代码

Java代码示例请参考同目录下的AndroidExample.kt文件
EOF

# 14. 生成Android示例代码
cat > "$WORK_DIR/final/AndroidExample.kt" << 'EOF'
import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/**
 * Linux环境管理器 - 在Android应用中使用
 */
class LinuxEnvironment(private val context: Context) {
    private val executor = Executors.newSingleThreadExecutor()
    private val rootfsDir = File(context.filesDir, "rootfs")
    private val prootFile: File
    
    init {
        // 创建rootfs目录
        if (!rootfsDir.exists()) {
            rootfsDir.mkdirs()
        }
        
        // 提取proot二进制文件
        prootFile = extractProot()
    }
    
    /**
     * 提取适合当前设备CPU架构的proot
     */
    private fun extractProot(): File {
        val arch = when {
            Build.SUPPORTED_ABIS.contains("arm64-v8a") -> "aarch64"
            Build.SUPPORTED_ABIS.contains("x86_64") -> "x86_64"
            else -> throw UnsupportedOperationException("不支持的CPU架构: ${Build.SUPPORTED_ABIS[0]}")
        }
        
        val prootAsset = "proot-$arch"
        val prootFile = File(context.filesDir, "proot")
        
        // 复制proot到私有目录
        context.assets.open(prootAsset).use { input ->
            FileOutputStream(prootFile).use { output ->
                input.copyTo(output)
            }
        }
        
        // 设置执行权限
        prootFile.setExecutable(true)
        
        return prootFile
    }
    
    /**
     * 确保rootfs已准备好
     */
    fun ensureRootfsReady(callback: (Boolean) -> Unit) {
        executor.execute {
            try {
                if (!isRootfsExtracted()) {
                    extractRootfs()
                }
                callback(true)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false)
            }
        }
    }
    
    /**
     * 检查rootfs是否已解压
     */
    private fun isRootfsExtracted(): Boolean {
        val checkFile = File(rootfsDir, "bin/busybox")
        return checkFile.exists()
    }
    
    /**
     * 解压rootfs
     */
    private fun extractRootfs() {
        val arch = when {
            Build.SUPPORTED_ABIS.contains("arm64-v8a") -> "arm64"
            Build.SUPPORTED_ABIS.contains("x86_64") -> "x86_64"
            else -> throw UnsupportedOperationException("不支持的CPU架构")
        }
        
        val rootfsAsset = "alpine-ide-rootfs-$arch.tar.xz"
        
        // 使用ProcessBuilder执行解压命令
        val pb = ProcessBuilder(
            "sh", "-c", 
            "mkdir -p ${rootfsDir.absolutePath} && " +
            "cat > ${context.filesDir.absolutePath}/rootfs.tar.xz && " +
            "cd ${rootfsDir.absolutePath} && " +
            "tar xf ${context.filesDir.absolutePath}/rootfs.tar.xz && " +
            "rm ${context.filesDir.absolutePath}/rootfs.tar.xz"
        )
        
        val process = pb.start()
        
        // 将asset文件传递给进程的标准输入
        context.assets.open(rootfsAsset).use { input ->
            process.outputStream.use { output ->
                input.copyTo(output)
            }
        }
        
        // 等待命令完成
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw RuntimeException("解压rootfs失败，退出码: $exitCode")
        }
    }
    
    /**
     * 编译并运行C++代码
     */
    fun compileAndRunCpp(
        code: String, 
        input: String, 
        callback: (success: Boolean, output: String, error: String) -> Unit
    ) {
        executor.execute {
            try {
                // 创建源代码文件
                val cppFile = File(context.filesDir, "source.cpp")
                cppFile.writeText(code)
                
                // 创建输入文件
                val inputFile = File(context.filesDir, "input.txt")
                inputFile.writeText(input)
                
                // 创建输出和错误文件
                val outputFile = File(context.filesDir, "output.txt")
                val errorFile = File(context.filesDir, "error.txt")
                
                // 清空输出文件
                if (outputFile.exists()) outputFile.delete()
                if (errorFile.exists()) errorFile.delete()
                outputFile.createNewFile()
                errorFile.createNewFile()
                
                // 构建编译命令
                val command = """
                    ${prootFile.absolutePath} 
                    -r ${rootfsDir.absolutePath} 
                    -b /dev 
                    -b /proc 
                    -b ${context.filesDir.absolutePath}:/external 
                    -w /external 
                    /usr/local/bin/compile-cpp 
                    /external/source.cpp 
                    /external/executable 
                    /external/input.txt 
                    /external/output.txt 
                    /external/error.txt
                """.trimIndent().replace("\n", " ")
                
                // 执行命令
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                process.waitFor()
                
                // 读取结果
                val output = if (outputFile.exists()) outputFile.readText() else ""
                val error = if (errorFile.exists()) errorFile.readText() else ""
                
                val success = process.exitValue() == 0 && error.isEmpty()
                
                // 返回结果
                callback(success, output, error)
                
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false, "", "执行错误: ${e.message}")
            }
        }
    }
    
    /**
     * 检查环境
     */
    fun checkEnvironment(callback: (String) -> Unit) {
        executor.execute {
            try {
                val command = """
                    ${prootFile.absolutePath} 
                    -r ${rootfsDir.absolutePath} 
                    -b /dev 
                    -b /proc 
                    /usr/local/bin/check-env
                """.trimIndent().replace("\n", " ")
                
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                
                callback(output)
            } catch (e: Exception) {
                e.printStackTrace()
                callback("检查环境失败: ${e.message}")
            }
        }
    }
}
EOF

echo "===== 创建完成 ====="
echo "最终文件位于: $WORK_DIR/final"
echo "ARM64 rootfs大小: $(du -h $WORK_DIR/final/assets/alpine-ide-rootfs-arm64.tar.xz | cut -f1)"
if [ -f "$WORK_DIR/final/assets/alpine-ide-rootfs-x86_64.tar.xz" ]; then
    echo "x86_64 rootfs大小: $(du -h $WORK_DIR/final/assets/alpine-ide-rootfs-x86_64.tar.xz | cut -f1)"
fi
echo "请将final/assets中的文件复制到Android项目的app/src/main/assets目录"
echo "请参考final/AndroidExample.kt实现Android应用中的集成"