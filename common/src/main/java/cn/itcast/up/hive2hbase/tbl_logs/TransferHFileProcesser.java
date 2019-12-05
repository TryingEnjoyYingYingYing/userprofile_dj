package cn.itcast.up.hive2hbase.tbl_logs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.HFileOutputFormat2;
import org.apache.hadoop.hbase.mapreduce.LoadIncrementalHFiles;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 常量类
 */
class Constants {
    //zk连接参数
    public static final String ZK_PARAM = "bd001:2181";
    //hive表数据目录
    public static final String INPUT_PATH = "hdfs://bd001:8020/user/hive/warehouse/tags_dat.db/tbl_logs";
    //生成的hfile目录
    public static final String HFILE_PATH = "hdfs://bd001:8020/output_hfile/tbl_logs2";
    //表名
    public static final String TABLE_NAME = "tbl_logs2";
    //表字段
    public static final List<String> list = new ArrayList<String>() {{
        add("id");
        add("log_id");
        add("remote_ip");
        add("site_global_ticket");
        add("site_global_session");
        add("global_user_id");
        add("cookie_text");
        add("user_agent");
        add("ref_url");
        add("loc_url");
        add("log_time");
    }};

}

/**
 * 将Hive表数据转换为HFile文件并移动HFile到HBase
 */
public class TransferHFileProcesser extends Configured implements Tool {

    private static Configuration configuration = HBaseConfiguration.create();
    private static Connection connection;

    public static void main(String[] args) throws Exception {
        configuration.set("hbase.zookeeper.quorum", Constants.ZK_PARAM);
        configuration.set("zookeeper.znode.parent", "/hbase-unsecure");
        connection = ConnectionFactory.createConnection(configuration);
        int run = ToolRunner.run(configuration, new TransferHFileProcesser(), args);
        System.out.println("HFile文件生成完毕!~~~");

        if(run == 0){
            Admin admin = connection.getAdmin();
            Table table = connection.getTable(TableName.valueOf(Constants.TABLE_NAME));

            LoadIncrementalHFiles load = new LoadIncrementalHFiles(configuration);
            load.doBulkLoad(new Path(Constants.HFILE_PATH), admin, table, connection.getRegionLocator(TableName.valueOf(Constants.TABLE_NAME)));
            System.out.println("HFile文件移动完毕!~~~");
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        Table table = connection.getTable(TableName.valueOf(Constants.TABLE_NAME));
        Job job = Job.getInstance(configuration);
        job.setJarByClass(TransferHFileProcesser.class);
        job.setMapperClass(LoadMapper.class);
        job.setMapOutputKeyClass(ImmutableBytesWritable.class);
        job.setMapOutputValueClass(Put.class);
        job.setOutputFormatClass(HFileOutputFormat2.class);

        HFileOutputFormat2.configureIncrementalLoad(job, table, connection.getRegionLocator(TableName.valueOf(Constants.TABLE_NAME)));
        FileInputFormat.addInputPath(job, new Path(Constants.INPUT_PATH));
        FileOutputFormat.setOutputPath(job, new Path(Constants.HFILE_PATH));

        boolean b = job.waitForCompletion(true);
        return b ? 0 : 1;
    }

}


class LoadMapper extends Mapper<LongWritable, Text, ImmutableBytesWritable, Put> {
    @Override
    protected void map(LongWritable key, Text value, Mapper.Context context) throws IOException, InterruptedException {
        String[] tmp = value.toString().split("\001");
        List<String> list = Arrays.asList(tmp);
        ArrayList<String> split = new ArrayList<>(list);
        if(split.size() == Constants.list.size() - 1) {
            //少一位.
            split.add("");
        }
        if (split.size() == Constants.list.size()) {
            Put put = new Put(Bytes.toBytes(split.get(0)));
            for (int i = 0; i < Constants.list.size(); i++) {
                put.addColumn("detail".getBytes(), Constants.list.get(i).getBytes(), Bytes.toBytes(split.get(i)));
            }
            context.write(new ImmutableBytesWritable(Bytes.toBytes(split.get(0))), put);
        }
    }
}