package com.springBatch0316.main.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

import com.springBatch0316.main.UserRegistration;

@Configuration
public class UserJob {

    private static final String INSERT_REGISTRATION_QUERY =
            "insert into USER_REGISTRATION (FIRST_NAME, LAST_NAME, COMPANY, ADDRESS,CITY,STATE,ZIP,COUNTY,URL,PHONE_NUMBER,FAX)" +
            " values " +
            "(:firstName,:lastName,:company,:address,:city,:state,:zip,:county,:url,:phoneNumber,:fax)";
    
    private static final String INSERT_COUNT = 
    		"insert into TOTAL_COUNT(COUNT) SELECT COUNT(NUM) FROM NURIBLOG";
    
    private Resource outputResource = new FileSystemResource("output/outputData.csv");
    
    @Autowired
    private JobBuilderFactory jobs;

    @Autowired
    private StepBuilderFactory steps;

    @Value("file:${user.home}/batches/registrations.csv")
    private Resource[] inputResources;
    
    @Autowired
    private DataSource dataSource;

    @Value("file:${user.home}/batches/registrations.csv")
    private Resource input;
    
    @Bean
    public Job insertIntoDbFromCsvJob() throws Exception {
        return jobs.get("User Registration Import Job")
                .start(step1())
                .build();
    }

    @Bean
    public FlatFileItemWriter<UserRegistration> writer() 
    {
        //Create writer instance
        FlatFileItemWriter<UserRegistration> writer = new FlatFileItemWriter<UserRegistration>();
         
        //Set output file location
        writer.setResource(outputResource);
         System.out.println(outputResource);
        //All job repetitions should "append" to same output file
        writer.setAppendAllowed(true);
         
        //Name field values sequence based on object properties 
        writer.setLineAggregator(new DelimitedLineAggregator<UserRegistration>() {
            {
                setDelimiter(",");
                setFieldExtractor(new BeanWrapperFieldExtractor<UserRegistration>() {
                    {
                        setNames(new String[] { "firstName", "lastName", "company", "address", "city", "state", "zip", "county", "url", "phoneNumber", "fax" });
                    }
                });
            }
        });
        return writer;
    }
    
    @Bean
    public Step step1() throws Exception {
        return steps.get("User Registration CSV To DB Step")
                .<UserRegistration,UserRegistration>chunk(5)
                .reader(csvFileReader())
                .writer(jdbcItemWriter())
                .build();
    }
    
    @Bean
    public Step step2() throws Exception {
        return steps.get("step2")
                .<UserRegistration,UserRegistration>chunk(5)
                .reader(multiResourceItemReader())
                .writer(writer())
                .build();
    }

    @Bean
    public Step step3() throws Exception {
        return steps.get("User Registration CSV To DB Step")
                .<UserRegistration,UserRegistration>chunk(5)
                .reader(multiResourceItemReader())
                .writer(jdbcItemWriter())
                .build();
    }
    
    
    @Bean
    public MultiResourceItemReader<UserRegistration> multiResourceItemReader() 
    {
        MultiResourceItemReader<UserRegistration> resourceItemReader = new MultiResourceItemReader<UserRegistration>();
        resourceItemReader.setResources(inputResources);
        resourceItemReader.setDelegate(reader());
        return resourceItemReader;
    }
    
    @Bean
    public FlatFileItemReader<UserRegistration> csvFileReader() throws Exception {

        return new FlatFileItemReaderBuilder<UserRegistration>()
                .name(ClassUtils.getShortName(FlatFileItemReader.class))
                .resource(input)
                .targetType(UserRegistration.class)
                .delimited()
                .names(new String[]{"firstName","lastName","company","address","city","state","zip","county","url","phoneNumber","fax"})
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<UserRegistration> jdbcItemWriter() {
        return new JdbcBatchItemWriterBuilder<UserRegistration>()
                    .dataSource(dataSource)
                    .sql(INSERT_REGISTRATION_QUERY)
                    .beanMapped()
                    .build();
    }
    
//    @Bean
//    public JdbcBatchItemWriter<UserRegistration> jdbcItemWriter1() {
//        return new JdbcBatchItemWriterBuilder<UserRegistration>()
//                    .dataSource(dataSource)
//                    .sql(INSERT_COUNT)
//                    .build();
//    }
    
    @Bean
    public FlatFileItemReader<UserRegistration> reader() 
    {
        //Create reader instance
        FlatFileItemReader<UserRegistration> reader = new FlatFileItemReader<UserRegistration>();
         
        //Set number of lines to skips. Use it if file has header rows.
        reader.setLinesToSkip(1);   
         
        //Configure how each line will be parsed and mapped to different values
        reader.setLineMapper(new DefaultLineMapper() {
            {
                //3 columns in each row
                setLineTokenizer(new DelimitedLineTokenizer() {
                    {
                        setNames(new String[] { "firstName","lastName","company","address","city","state","zip","county","url","phoneNumber","fax"});
                    }
                });
                //Set values in Employee class
                setFieldSetMapper(new BeanWrapperFieldSetMapper<UserRegistration>() {
                    {
                        setTargetType(UserRegistration.class);
                    }
                });
            }
        });
        return reader;
    }
    
}
