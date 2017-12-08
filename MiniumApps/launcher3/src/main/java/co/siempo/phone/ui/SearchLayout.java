package co.siempo.phone.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.widget.CardView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.eyeem.chips.BubbleStyle;
import com.eyeem.chips.ChipsEditText;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.Trace;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import co.siempo.phone.MainActivity;
import co.siempo.phone.R;
//import co.siempo.phone.app.Launcher3Prefs_;
import co.siempo.phone.event.NotificationTrayEvent;
import co.siempo.phone.event.SearchLayoutEvent;
import co.siempo.phone.token.TokenCompleteType;
import co.siempo.phone.token.TokenItem;
import co.siempo.phone.token.TokenItemType;
import co.siempo.phone.token.TokenManager;
import co.siempo.phone.token.TokenUpdateEvent;
import de.greenrobot.event.EventBus;
import de.greenrobot.event.Subscribe;
import minium.co.core.util.UIUtils;

import static minium.co.core.log.LogConfig.TRACE_TAG;

/**
 * Created by Shahab on 2/16/2017.
 */
public class SearchLayout extends CardView {

    public ChipsEditText getTxtSearchBox() {
        return txtSearchBox;
    }

    private SharedPreferences launcherPrefs;

    public ChipsEditText txtSearchBox;

    ImageView btnClear;


    private View inflateLayout;

    private String formattedTxt;
    private boolean isWatching = true;
    private Handler handler;

    public SearchLayout(Context context) {
        super(context);
        init(context);
    }

    public SearchLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SearchLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }


    private void init(Context context) {
        isWatching = true;
        inflateLayout = inflate(context, R.layout.search_layout, this);

        launcherPrefs = context.getSharedPreferences("Launcher3Prefs", 0);
        txtSearchBox= inflateLayout.findViewById(R.id.txtSearchBox);
        btnClear= inflateLayout.findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                txtSearchBox.setText("");
            }
        });

        setCardElevation(4.0f);
        handler = new Handler();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setupViews();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    void setupViews() {
        txtSearchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handleAfterTextChanged(s.toString());
                MainActivity.isTextLenghGreater = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    public void askFocus() {

        if (MainActivity.isTextLenghGreater.length() > 0) {
            MainActivity.isTextLenghGreater = MainActivity.isTextLenghGreater.trim();
            handleAfterTextChanged(MainActivity.isTextLenghGreater);
        } else {
            if(launcherPrefs.getBoolean("isKeyBoardDisplay",false))
                 txtSearchBox.requestFocus();
            btnClear.setVisibility(INVISIBLE);
            txtSearchBox.setText("");
        }
        handler.postDelayed(showKeyboardRunnable, 500);
    }

    private Runnable showKeyboardRunnable = new Runnable() {
        @Override
        public void run() {
                if(launcherPrefs.getBoolean("isKeyBoardDisplay",false)) {
                    UIUtils.showKeyboard(txtSearchBox);
                }
        }
    };

    private void handleAfterTextChanged(String s) {
        if (s.length() != 0) {
            btnClear.setVisibility(VISIBLE);
        } else {
            btnClear.setVisibility(INVISIBLE);
        }

        if (isWatching) {
            EventBus.getDefault().post(new SearchLayoutEvent(s.toString()));
        }
    }


    @Subscribe
    public void tokenManagerEvent(TokenUpdateEvent event) {
        buildFormattedText();
        updateSearchField();
    }

    @Subscribe
    public void notificationHideEvent(NotificationTrayEvent event) {
        if (!event.isVisible())
            askFocus();
    }

    private void updateSearchField() {
        String[] splits = formattedTxt.split("\\|");

        String newText = "";
        boolean space = false;
        for (String s : splits) {
            if (space) {
                if (!s.startsWith(" "))
                    newText += " ";
            }
            space = true;
            newText += s.replaceAll("\\^", "").replaceAll("~", "");
        }

        if (formattedTxt.endsWith("|")) newText += " ";

        isWatching = false;
        txtSearchBox.setText(newText);
        isWatching = true;

        int startPos = 0;
        int endPos = 0;
        for (String s : splits) {
            endPos += s.length();
            if (s.startsWith("^")) {
                txtSearchBox.setCurrentBubbleStyle(BubbleStyle.build(getContext(), R.style.bubble_style_selected));
                txtSearchBox.makeChip(startPos, endPos - 1, false, null);
            } else if (s.startsWith("~")) {
                txtSearchBox.setCurrentBubbleStyle(BubbleStyle.build(getContext(), R.style.bubble_style_empty));
                txtSearchBox.makeChip(startPos, endPos - 1, false, null);
            } else {
                endPos++; // space
            }

            startPos = endPos;
        }
        txtSearchBox.setSelection(newText.length());
    }

    private void buildFormattedText() {
        formattedTxt = "";

        for (TokenItem item : TokenManager.getInstance().getItems()) {
            if (item.getCompleteType() == TokenCompleteType.FULL) {
                if (item.isChipable()) {
                    formattedTxt += "^";
                }

                if (item.getItemType() == TokenItemType.CONTACT) {
                    //formattedTxt += "@";
                }

                formattedTxt += item.getTitle() + "|";
            } else if (item.getCompleteType() == TokenCompleteType.HALF) {
                if (item.isChipable()) {
                    formattedTxt += "~";
                }
                formattedTxt += item.getTitle() + "|";

            } else {
                formattedTxt += item.getTitle();
            }
        }
    }
}