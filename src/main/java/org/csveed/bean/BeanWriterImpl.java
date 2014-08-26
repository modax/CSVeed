package org.csveed.bean;

import org.csveed.bean.conversion.BeanWrapper;
import org.csveed.bean.conversion.ConversionException;
import org.csveed.bean.conversion.DefaultConverters;
import org.csveed.row.*;

import java.util.Collection;

public class BeanWriterImpl<T> implements BeanWriter<T> {

    private final RowWriter rowWriter;

    private final BeanInstructions beanInstructions;

    private boolean headerWritten;

    private DefaultConverters defaultConverters = new DefaultConverters();

    private HeaderImpl header;

    public BeanWriterImpl(RowWriter rowWriter, Class<T> beanClass) {
        this(rowWriter, new BeanParser().getBeanInstructions(beanClass));
    }

    public BeanWriterImpl(RowWriter rowWriter, BeanInstructions beanInstructions) {
        this.beanInstructions = beanInstructions;
        this.rowWriter = rowWriter;
    }

    @Override
    public void writeBeans(Collection<T> beans) {
        for (T bean : beans) {
            writeBean(bean);
        }
    }

    @Override
    public void writeBean(T bean) {
        writeHeader();

        LineWithInfo line = new LineWithInfo();

        BeanWrapper beanWrapper = new BeanWrapper(defaultConverters, bean);
        for (BeanProperty property : beanInstructions.getProperties()) {
            try {
                line.addCell(beanWrapper.getProperty(property));
            } catch (ConversionException e) {
                System.out.println(e.getMessage());
            }
        }
        rowWriter.writeRow(new RowImpl(line, header));
    }

    private void writeHeader() {
        if (!beanInstructions.useHeader() || headerWritten) {
            return;
        }
        LineWithInfo line = new LineWithInfo();
        for (BeanProperty property : beanInstructions.getProperties()) {
            line.addCell(property.getColumnName());
        }
        header = new HeaderImpl(line);
        rowWriter.writeHeader(header);
        headerWritten = true;
    }

    @Override
    public RowWriter getRowWriter() {
        return rowWriter;
    }

}
