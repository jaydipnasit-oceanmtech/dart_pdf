/*
 * Copyright (C) 2017, David PHAM-VAN <dev.nfet.net@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nfet.flutter.printing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * PrintingPlugin
 */
public class PrintingPlugin implements MethodCallHandler {
    private static PrintManager printManager;
    private final Activity activity;

    private PrintingPlugin(Activity activity) {
        this.activity = activity;
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "printing");
        channel.setMethodCallHandler(new PrintingPlugin(registrar.activity()));
        printManager = (PrintManager) registrar.activity().getSystemService(Context.PRINT_SERVICE);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "printPdf":
                printPdf((byte[]) call.argument("doc"));
                result.success(0);
                break;
            case "sharePdf":
                sharePdf((byte[]) call.argument("doc"));
                result.success(0);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private void printPdf(final byte[] documentData) {
        PrintDocumentAdapter pda = new PrintDocumentAdapter() {
            PrintedPdfDocument mPdfDocument;

            @Override
            public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor parcelFileDescriptor,
                    CancellationSignal cancellationSignal,
                    WriteResultCallback writeResultCallback) {
                OutputStream output = null;
                try {
                    output = new FileOutputStream(parcelFileDescriptor.getFileDescriptor());
                    output.write(documentData, 0, documentData.length);
                    writeResultCallback.onWriteFinished(new PageRange[] {PageRange.ALL_PAGES});
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (output != null) {
                            output.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                    CancellationSignal cancellationSignal, LayoutResultCallback callback,
                    Bundle extras) {
                // Create a new PdfDocument with the requested page attributes
                mPdfDocument = new PrintedPdfDocument(activity, newAttributes);

                // Respond to cancellation request
                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }

                // Return print information to print framework
                PrintDocumentInfo info =
                        new PrintDocumentInfo.Builder("document.pdf")
                                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                                .build();
                // Content layout reflow is complete
                callback.onLayoutFinished(info, true);
            }

            @Override
            public void onFinish() {
                // noinspection ResultOfMethodCallIgnored
            }
        };
        String jobName = "Document";
        printManager.print(jobName, pda, null);
    }

    private void sharePdf(byte[] data) {
        try {
            final File externalFilesDirectory =
                    activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File shareFile = File.createTempFile("document", ".pdf", externalFilesDirectory);

            FileOutputStream stream = new FileOutputStream(shareFile);
            stream.write(data);
            stream.close();

            Uri apkURI = FileProvider.getUriForFile(activity,
                    activity.getApplicationContext().getPackageName() + ".flutter.printing",
                    shareFile);

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, apkURI);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooserIntent = Intent.createChooser(shareIntent, null);
            activity.startActivity(chooserIntent);
            shareFile.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}