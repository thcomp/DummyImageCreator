package jp.co.thcomp.dummyimagecreator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private static final int ViewTagAvailableColor = "ViewTagAvailableColor".hashCode();

    private View mProcessingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((Spinner)findViewById(R.id.spnrTextColor)).setAdapter(new ColorSpinnerAdapter(this));

        mProcessingView = findViewById(R.id.rlProcessing);
        mProcessingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        findViewById(R.id.btnCreateDummyImages).setOnClickListener(mClickListener);
    }

    private String createDummyImages(DummyImageParams params){
        int rangeStart = Math.min(params.rangeStart, params.rangeEnd);
        int rangeEnd = Math.max(params.rangeStart, params.rangeEnd);
        Bitmap baseBitmap = Bitmap.createBitmap(params.imageWidth, params.imageHeight, Bitmap.Config.ARGB_8888);
        Canvas baseCanvas = new Canvas(baseBitmap);
        Paint paint = new Paint();
        int frameSize = getResources().getDimensionPixelSize(R.dimen.dummy_image_frame_size);
        if(frameSize > Math.min(params.imageWidth, params.imageHeight)/20){
            frameSize = Math.min(params.imageWidth, params.imageHeight)/20;
        }

        RectF frameInnerRect = new RectF(frameSize, frameSize, params.imageWidth - frameSize, params.imageHeight - frameSize);
        String fileNameFormat = "%0" + String.valueOf(rangeEnd).length() + "d.png";
        Path baseDrawablePath = new Path();
        baseDrawablePath.addRect(frameInnerRect, Path.Direction.CW);
        baseDrawablePath.setFillType(Path.FillType.INVERSE_EVEN_ODD);
        paint.setColor(params.textColor);

        Calendar currentCalendar = Calendar.getInstance();
        File saveImageFolderPath = new File(MainActivity.this.getExternalFilesDir(null).getAbsolutePath() + "/" +
                String.format("%d-%02d-%02d_%02d%02d%02d", currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH) + 1, currentCalendar.get(Calendar.DAY_OF_MONTH), currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE), currentCalendar.get(Calendar.SECOND)));
        saveImageFolderPath.mkdirs();

        try {
            for (int i = rangeStart; i <= rangeEnd; i++) {
                baseCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                baseCanvas.save();
                baseCanvas.clipPath(baseDrawablePath);
                baseCanvas.drawColor(params.textColor);
                baseCanvas.restore();

                String valueStr = String.valueOf(i);
                int fontSize = (params.imageWidth - (frameSize * 4)) / valueStr.length();
                if(fontSize <= 0){
                    fontSize = (params.imageWidth - (frameSize * 2)) / valueStr.length();
                    if(fontSize <= 0){
                        fontSize = params.imageWidth / valueStr.length();
                    }
                }else {
                    if (fontSize > params.imageHeight) {
                        fontSize = params.imageHeight;
                    }
                }

                paint.setTextSize(fontSize);
                int textSize = (int)Math.ceil(paint.measureText(valueStr));
                Bitmap numberBitmap = null;
                try{
                    numberBitmap = Bitmap.createBitmap(textSize, fontSize, Bitmap.Config.ARGB_8888);
                    Canvas numberCavans = new Canvas(numberBitmap);
                    numberCavans.drawText(valueStr, 0, Math.abs(paint.ascent()), paint);

                    baseCanvas.drawBitmap(
                            numberBitmap,
                            baseCanvas.getWidth()/2 - numberCavans.getWidth()/2,
                            baseCanvas.getHeight()/2 - numberCavans.getHeight()/2,
                            null);
                }finally {
                    if(numberBitmap != null) {
                        numberBitmap.recycle();
                    }
                }

                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(saveImageFolderPath.getAbsoluteFile() + "/" + String.format(fileNameFormat, i));
                    baseBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if(outputStream != null){
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }finally {
            if(baseBitmap != null) {
                baseBitmap.recycle();
            }
        }

        return saveImageFolderPath.getAbsolutePath();
    }

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();

            if(id == R.id.btnCreateDummyImages){
                String imageWidthStr = ((EditText)findViewById(R.id.etImageWidth)).getText().toString();
                String imageHeightStr = ((EditText)findViewById(R.id.etImageHeight)).getText().toString();
                String rangeStartStr = ((EditText)findViewById(R.id.etRangeStart)).getText().toString();
                String rangeEndStr = ((EditText)findViewById(R.id.etRangeEnd)).getText().toString();

                try{
                    final DummyImageParams params = new DummyImageParams();
                    params.imageWidth = Integer.parseInt(imageWidthStr);
                    params.imageHeight = Integer.parseInt(imageHeightStr);
                    params.rangeStart = Integer.parseInt(rangeStartStr);
                    params.rangeEnd = Integer.parseInt(rangeEndStr);
                    params.textColor = ((ColorSpinnerAdapter.AvailableColor)(((Spinner)findViewById(R.id.spnrTextColor)).getSelectedItem())).getColor();

                    mProcessingView.setVisibility(View.VISIBLE);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final String imageDirPath = createDummyImages(params);
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mProcessingView.setVisibility(View.GONE);
                                    Toast.makeText(MainActivity.this, "create images in " + imageDirPath, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }).start();
                }catch(NumberFormatException e){
                    Toast.makeText(MainActivity.this, "input illegal value", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private static class ColorSpinnerAdapter extends BaseAdapter {
        private static enum AvailableColor{
            Black(Color.BLACK),
            Blue(Color.BLUE),
            Cyan(Color.CYAN),
            DarkGray(Color.DKGRAY),
            Gray(Color.GRAY),
            Green(Color.GREEN),
            LightGray(Color.LTGRAY),
            Magenta(Color.MAGENTA),
            Red(Color.RED),
            Transparent(Color.TRANSPARENT),
            White(Color.WHITE),
            Yellow(Color.YELLOW),
            ;

            private int mColorValue;

            AvailableColor(int colorValue){
                mColorValue = colorValue;
            }

            public int getColor(){
                return mColorValue;
            }
        }

        private Context mContext;

        public ColorSpinnerAdapter(Context context){
            mContext = context;
        }

        @Override
        public int getCount() {
            return AvailableColor.values().length;
        }

        @Override
        public Object getItem(int position) {
            return AvailableColor.values()[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            AvailableColor availableColor = (AvailableColor)getItem(position);
            int colorValue = availableColor.getColor();
            View ret = convertView;

            if(ret == null){
                LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                ret = inflater.inflate(R.layout.item_available_color, parent, false);
            }

            TextView tvAvailableColor = (TextView)ret.findViewById(R.id.tvAvailableColor);
            tvAvailableColor.setText(availableColor.name());
            tvAvailableColor.setTextColor((~colorValue) | 0xFF000000);
            ret.setBackgroundColor(colorValue);

            ret.setTag(ViewTagAvailableColor, availableColor);

            return ret;
        }
    }

    private static class DummyImageParams{
        int imageWidth;
        int imageHeight;
        int rangeStart;
        int rangeEnd;
        int textColor;
    }
}
