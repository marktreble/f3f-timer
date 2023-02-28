/*
 *     ___________ ______   _______
 *    / ____/__  // ____/  /_  __(_)___ ___  ___  _____
 *   / /_    /_ </ /_       / / / / __ `__ \/ _ \/ ___/
 *  / __/  ___/ / __/      / / / / / / / / /  __/ /
 * /_/    /____/_/        /_/ /_/_/ /_/ /_/\___/_/
 *
 * Open Source F3F timer UI and scores database
 *
 */

package com.marktreble.f3ftimer.printing;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.util.SparseIntArray;

import com.marktreble.f3ftimer.data.pilot.Pilot;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PilotListDocumentAdapter extends PrintDocumentAdapter {

    private final Context mContext;
    private final ArrayList<Pilot> mPilots;
    private final String mRaceName;
    private float mPageHeight;
    private float mPageWidth;
    private int mNumPages;
    private PrintAttributes mAttributes;

    private final static int ROW_HEIGHT = 27; // 3/8 inch
    private final static int TOP_MARGIN = 72; // 1/2 Inch
    private final static int ATTEMPTED_BOTTOM_MARGIN = 36; // 1/2 Inch
    private final static int HORIZONTAL_MARGIN = 36; // 1/2 Inch
    private final static int COLUMN_PADDING = 8;

    private final static int NUM_TIME_COLUMNS = 8;
    private final static int NUM_NAME_COLUMNS = 3;


    public PilotListDocumentAdapter(Context context, ArrayList<Pilot> pilots, String racename) {
        mContext = context;
        mPilots = pilots;
        mRaceName = racename;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes,
                         PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback,
                         Bundle metadata) {

        mAttributes = newAttributes;

        // Respond to cancellation request
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        // Get the page width/height and orientation
        PrintAttributes.MediaSize pageSize = newAttributes.getMediaSize();
        if (pageSize == null) {
            callback.onLayoutFailed("Page count calculation failed.");
            return;
        }
        getMetrics(pageSize);


        // Compute the expected number of printed pages
        mNumPages = computePageCount();

        if (mNumPages > 0) {
            // Return print information to print framework
            PrintDocumentInfo info = new PrintDocumentInfo
                    .Builder(mRaceName + ".pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(mNumPages)
                    .build();
            // Content layout reflow is complete
            callback.onLayoutFinished(info, true);
        } else {
            // Otherwise report an error to the print framework
            callback.onLayoutFailed("Page count calculation failed.");
        }
    }

    private void getMetrics(PrintAttributes.MediaSize pageSize) {
        mPageHeight = ((float) pageSize.getHeightMils() * 72) / 1000;
        mPageWidth = ((float) pageSize.getWidthMils() * 72) / 1000;
    }

    private int computePageCount() {
        float listHeight = mPageHeight - (TOP_MARGIN + ATTEMPTED_BOTTOM_MARGIN);
        double numRows = Math.floor(listHeight / (float) ROW_HEIGHT);
        double numPilotRows = numRows - 1; // -1 to leave top row blank

        return (int) Math.ceil((float) mPilots.size() / numPilotRows);
    }

    @Override
    public void onWrite(final PageRange[] pages,
                        final ParcelFileDescriptor destination,
                        final CancellationSignal cancellationSignal,
                        final WriteResultCallback callback) {

        // Create a new PdfDocument with the requested page attributes
        PrintedPdfDocument pdfDocument = new PrintedPdfDocument(mContext, mAttributes);
        final SparseIntArray writtenPagesArray = new SparseIntArray();
        // Iterate over each page of the document,
        // check if it's in the output range.
        for (int i = 0; i < mNumPages; i++) {
            // Check to see if this page is in the output range.
            if (containsPage(pages, i)) {
                // If so, add it to writtenPagesArray. writtenPagesArray.size()
                // is used to compute the next output page index.
                writtenPagesArray.append(writtenPagesArray.size(), i);
                PdfDocument.Page page = pdfDocument.startPage(i);

                // check for cancellation
                if (cancellationSignal.isCanceled()) {
                    callback.onWriteCancelled();
                    pdfDocument.close();
                    return;
                }

                // Draw page content for printing
                drawPage(page, i);

                // Rendering is complete, so page can be finalized.
                pdfDocument.finishPage(page);
            }
        }

        // Write PDF document to file
        try {
            pdfDocument.writeTo(new FileOutputStream(
                    destination.getFileDescriptor()));
        } catch (IOException e) {
            callback.onWriteFailed(e.toString());
            return;
        } finally {
            pdfDocument.close();
        }

        List<PageRange> pageRanges = new ArrayList<>();
        int start;
        int end;
        final int writtenPageCount = writtenPagesArray.size();
        for (int i = 0; i < writtenPageCount; i++) {
            start = writtenPagesArray.valueAt(i);
            int oldEnd = end = start;
            while (i < writtenPageCount && (end - oldEnd) <= 1) {
                oldEnd = end;
                end = writtenPagesArray.valueAt(i);
                i++;
            }
            if (start >= 0 && end >= 0) {
                PageRange pageRange = new PageRange(start, end);
                pageRanges.add(pageRange);
            }
        }

        // Signal the print framework the document is complete
        PageRange[] writtenPages = new PageRange[pageRanges.size()];
        pageRanges.toArray(writtenPages);
        callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
    }

    private boolean containsPage(PageRange[] pageRanges, int page) {
        for (PageRange pr : pageRanges) {
            if (pr.getStart() <= page
                    && page <= pr.getEnd()) {
                return true;
            }
        }
        return false;
    }

    private void drawPage(PdfDocument.Page page, int pageNum) {
        Canvas canvas = page.getCanvas();

        float listHeight = mPageHeight - (TOP_MARGIN + ATTEMPTED_BOTTOM_MARGIN);
        double numRows = Math.floor(listHeight / (float) ROW_HEIGHT);
        double numPilotRows = numRows - 1;

        int bottom = (int) (TOP_MARGIN + (numRows * ROW_HEIGHT));

        Paint paint = new Paint();
        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(1);

        drawVertical(
                canvas,
                paint,
                HORIZONTAL_MARGIN,
                bottom);

        for (int i = 0; i <= NUM_TIME_COLUMNS; i++) {
            drawVertical(
                    canvas,
                    paint,
                    (int) (mPageWidth - HORIZONTAL_MARGIN - (int) (i * (((mPageWidth - (HORIZONTAL_MARGIN * 2)) / (NUM_TIME_COLUMNS + NUM_NAME_COLUMNS))))),
                    bottom);
        }

        for (int i = 0; i <= numRows; i++) {
            drawHorizontal(
                    canvas,
                    paint,
                    TOP_MARGIN + (i * ROW_HEIGHT));
        }


        paint.setTextSize(14);
        paint.setColor(Color.BLACK);

        int start = (int) numPilotRows * pageNum;
        int end = (int) Math.min(mPilots.size(), start + numPilotRows);

        int maxTextWidth = (int) (NUM_NAME_COLUMNS * (((mPageWidth - (HORIZONTAL_MARGIN * 2)) / (NUM_TIME_COLUMNS + NUM_NAME_COLUMNS)))) - (COLUMN_PADDING * 2);

        for (int i = start; i < end; i++) {
            String name = String.format("%s. %s %s ", mPilots.get(i).number, mPilots.get(i).firstname, mPilots.get(i).lastname);
            int text_width = maxTextWidth + 1;
            int text_height = 0;

            while (text_width > maxTextWidth) {
                name = name.substring(0, name.length() - 1);
                Rect bounds = new Rect();
                paint.getTextBounds(name, 0, name.length(), bounds);

                text_height = bounds.height();
                text_width = bounds.width();
            }

            int baseline = (ROW_HEIGHT - text_height) / 2;

            int offsetY = (i - start) * ROW_HEIGHT;


            canvas.drawText(
                    name,
                    HORIZONTAL_MARGIN + COLUMN_PADDING,
                    TOP_MARGIN + ROW_HEIGHT + ROW_HEIGHT + offsetY - baseline,
                    paint);
        }


    }

    private void drawVertical(Canvas canvas, Paint paint, int x, int b) {
        canvas.drawLine(x, TOP_MARGIN, x, b, paint);
    }

    private void drawHorizontal(Canvas canvas, Paint paint, int y) {
        canvas.drawLine(HORIZONTAL_MARGIN, y, mPageWidth - HORIZONTAL_MARGIN, y, paint);
    }

}
