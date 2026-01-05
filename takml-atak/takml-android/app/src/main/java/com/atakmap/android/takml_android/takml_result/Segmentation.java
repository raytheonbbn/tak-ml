package com.atakmap.android.takml_android.takml_result;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Segmentation extends TakmlResult {

    private static final String TAG = Segmentation.class.getSimpleName();
    private Bitmap bitmap;
    private final Set<String> labels = new TreeSet<>();
    private List<Bitmap> chips = new ArrayList<>();

    private int coordW, coordH;
    List<float[]> coordinates;

    public Segmentation(){

    }

    public Segmentation(Parcel parcel) {
        byte[] bitmapBytes = parcel.readBlob();
        bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
        //bitmap = (Bitmap)parcel.readValue(Bitmap.class.getClassLoader());
        //bitmap = parcel.readParcelable(Bitmap.class.getClassLoader(), Bitmap.class);
        try {
            //Bitmap chip = parcel.readParcelable(Bitmap.class.getClassLoader(), Bitmap.class);
            //Bitmap chip = (Bitmap)parcel.readValue(Bitmap.class.getClassLoader());
            byte[] chipBitmapBytes = parcel.readBlob();
            if(chipBitmapBytes != null) {
                chips.add(BitmapFactory.decodeByteArray(chipBitmapBytes, 0, chipBitmapBytes.length));
            }
        } catch (Exception e) {
            // ** there might not be any chips
        }
    }

    public List<Bitmap> getChips() {
        return chips;
    }

    public void setChips(List<Bitmap> chips) {
        this.chips = chips;
    }

    public void addChip(Bitmap chipBitmap) {
        if (chips == null) {
            chips = new ArrayList<>();
        }

        chips.add(chipBitmap);
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void addLabel(String label) {
        labels.add(label);
    }

    public Set<String> getLabels() {
        return labels;
    }

    public int getCoordW() {
        return coordW;
    }

    public void setCoordW(int coordW) {
        this.coordW = coordW;
    }

    public int getCoordH() {
        return coordH;
    }

    public void setCoordH(int coordH) {
        this.coordH = coordH;
    }

    public List<float[]> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<float[]> coordinates) {
        this.coordinates = coordinates;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Writes the object's data to the Parcel.
     * All data written must be read in the same order in the constructor Segmentation(Parcel).
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        Log.d(TAG, "Writing segmentation to parcel");

        // 1. Write 'bitmap'
        try {
            if(bitmap != null) {
                Log.d(TAG, "Writing bitmap of size " + bitmap.getByteCount() + " to parcel");
                dest.writeBlob(bitmapToByteArray(bitmap));
            } else {
                dest.writeBlob(null); // Write null blob if bitmap is null
            }
        } catch (Exception e) {
            Log.e(TAG, "Error writing bitmap to parcel", e);
            dest.writeBlob(null); // Ensure something is written even on error
        }

        // 2. Write 'labels' (Set<String>)
        dest.writeStringList(new ArrayList<>(labels));

        // 3. Write 'chips' (List<Bitmap>)
        if(chips != null) {
            dest.writeInt(chips.size()); // Write number of chips
            for (Bitmap chip : chips) {
                try {
                    if (chip != null) {
                        dest.writeBlob(bitmapToByteArray(chip));
                    } else {
                        dest.writeBlob(null);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error writing chip bitmap to parcel", e);
                    dest.writeBlob(null);
                }
            }
        } else {
            dest.writeInt(0); // Write 0 if chips list is null
        }

        // 4. Write 'coordW' and 'coordH'
        dest.writeInt(coordW);
        dest.writeInt(coordH);

        // 5. Write 'coordinates' (List<float[]>)
        if (coordinates != null) {
            dest.writeInt(coordinates.size()); // Write number of float arrays
            for (float[] floatArray : coordinates) {
                if (floatArray != null) {
                    dest.writeInt(floatArray.length); // Write the size of the float array
                    dest.writeFloatArray(floatArray); // Write the float array
                } else {
                    dest.writeInt(0); // Write 0 size if array is null
                    dest.writeFloatArray(new float[0]);
                }
            }
        } else {
            dest.writeInt(0); // Write 0 if coordinates list is null
        }

        Log.d(TAG, "Wrote segmentation to parcel");
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream);
        return stream.toByteArray();
    }

    public static final Creator<Segmentation> CREATOR = new Creator<Segmentation>() {
        @Override
        public Segmentation createFromParcel(Parcel in) {
            return new Segmentation(in);
        }

        @Override
        public Segmentation[] newArray(int size) {
            return new Segmentation[size];
        }
    };
}
