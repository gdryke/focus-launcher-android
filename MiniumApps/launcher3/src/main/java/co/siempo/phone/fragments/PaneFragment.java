package co.siempo.phone.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.eyeem.chips.ChipsEditText;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import co.siempo.phone.R;
import co.siempo.phone.activities.CoreActivity;
import co.siempo.phone.adapters.MainListAdapter;
import co.siempo.phone.adapters.PanePagerAdapter;
import co.siempo.phone.adapters.ToolsMenuAdapter;
import co.siempo.phone.app.CoreApplication;
import co.siempo.phone.customviews.ItemOffsetDecoration;
import co.siempo.phone.customviews.SearchLayout;
import co.siempo.phone.event.SearchLayoutEvent;
import co.siempo.phone.log.Tracer;
import co.siempo.phone.main.MainFragmentMediator;
import co.siempo.phone.main.MainListItemLoader;
import co.siempo.phone.models.AppMenu;
import co.siempo.phone.models.MainListItem;
import co.siempo.phone.token.TokenCompleteType;
import co.siempo.phone.token.TokenItem;
import co.siempo.phone.token.TokenItemType;
import co.siempo.phone.token.TokenManager;
import co.siempo.phone.token.TokenParser;
import co.siempo.phone.token.TokenRouter;
import co.siempo.phone.token.TokenUpdateEvent;
import co.siempo.phone.utils.PackageUtil;
import co.siempo.phone.utils.PrefSiempo;
import de.greenrobot.event.Subscribe;
import me.relex.circleindicator.CircleIndicator;

/**
 * Main class for Tools Pane, Favorites Pane and JunkFood Pane.
 * Ui is changed based on which pane the user is currently on.
 * 1. Tools Pane
 * 2. Favourites Pane
 * 3. Junkfood Pane
 */
public class PaneFragment extends CoreFragment implements View.OnClickListener {

    private LinearLayout linTopDoc;
    private ViewPager pagerPane;
    private LinearLayout linPane;
    private LinearLayout linBottomDoc;
    private EditText edtSearchToolsRounded;
    private TextView txtTopDockDate;
    private View linSearchList;
    private SearchLayout searchLayout;
    private RelativeLayout relSearchTools;
    private ListView listView;
    private CardView cardViewEdtSearch;
    private View blueLineDivider;
    private TextView txtIntentionLabelJunkPane;
    private TextView txtIntention;
    private Window mWindow;
    private int defaultStatusBarColor;
    private MainFragmentMediator mediator;
    private TokenRouter router;
    private MainListAdapter adapter;
    private TokenParser parser;

    private RecyclerView recyclerViewBottomDoc;
    private List<MainListItem> items = new ArrayList<>();
    private ToolsMenuAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ItemOffsetDecoration itemDecoration;

    /**
     * Edit Text inside the SearchLayout
     */
    private ChipsEditText chipsEditText;
    /**
     * Clear button inside the SearchLayout
     */
    private ImageView imageClear;
    private View rootView;
    private InputMethodManager inputMethodManager;

    public PaneFragment() {
        // Required empty public constructor
    }

    public static PaneFragment newInstance() {
        return new PaneFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_pane, container, false);
        context = (CoreActivity) getActivity();
        initView(rootView);
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mediator = new MainFragmentMediator(this);

        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                loadData();
            }
        });
    }

    public void loadData() {
        mediator.loadData();
        loadView();
    }

    public void loadView() {
        if (getActivity() != null) {
            adapter = new MainListAdapter(getActivity(), mediator.getItems());
            listView.setAdapter(adapter);
            adapter.getFilter().filter(TokenManager.getInstance().getCurrent().getTitle());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            adapter.getFilter().filter("");
        }
    }


    private void initView(View view) {
        linTopDoc = view.findViewById(R.id.linTopDoc);
        linTopDoc.setOnClickListener(this);
        linPane = view.findViewById(R.id.linPane);
        edtSearchToolsRounded = view.findViewById(R.id.edtSearchTools);
        blueLineDivider = view.findViewById(R.id.blueLineView);
        cardViewEdtSearch = view.findViewById(R.id.cardViewEdtSearch);
        searchLayout = view.findViewById(R.id.edtSearchListView);
        relSearchTools = view.findViewById(R.id.relSearchTools);
        txtTopDockDate = view.findViewById(R.id.txtTopDockDate);
        txtIntentionLabelJunkPane = view.findViewById(R.id.txtIntentionLabelJunkPane);
        txtIntention = view.findViewById(R.id.txtIntention);
        linPane.setOnClickListener(this);
        linBottomDoc = view.findViewById(R.id.linBottomDoc);
        linSearchList = view.findViewById(R.id.linSearchList);
        listView = view.findViewById(R.id.listView);
        linBottomDoc.setOnClickListener(this);
        CircleIndicator indicator = view.findViewById(R.id.indicator);
        pagerPane = view.findViewById(R.id.pagerPane);
        chipsEditText = searchLayout.getTxtSearchBox();
        imageClear = searchLayout.getBtnClear();

        PanePagerAdapter mPagerAdapter = new PanePagerAdapter(getChildFragmentManager());
        pagerPane.setAdapter(mPagerAdapter);
        indicator.setViewPager(pagerPane);
        pagerPane.setCurrentItem(2);

        bindBottomDoc();
        inputMethodManager = (InputMethodManager) getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        txtTopDockDate.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (null != imageClear && imageClear.getVisibility() == View
                        .VISIBLE) {
                    imageClear.performClick();
                }
                return true;
            }
        });
        resetSearchList();
    }

    private void bindBottomDoc() {
        ArrayList<MainListItem> itemsLocal = new ArrayList<>();
        new MainListItemLoader(getActivity()).loadItemsDefaultApp(itemsLocal);
        itemsLocal = PackageUtil.getToolsMenuData(getActivity(), itemsLocal);
        Set<Integer> list = new HashSet<>();

        for (Map.Entry<Integer, AppMenu> entry : CoreApplication.getInstance().getToolsSettings().entrySet()) {
            if (entry.getValue().isBottomDoc()) {
                list.add(entry.getKey());
            }
        }

        for (MainListItem mainListItem : itemsLocal) {
            if (list.contains(mainListItem.getId())) {
                items.add(mainListItem);
            }
        }

        recyclerViewBottomDoc = rootView.findViewById(R.id.recyclerViewBottomDoc);
        mLayoutManager = new GridLayoutManager(getActivity(), 4);
        recyclerViewBottomDoc.setLayoutManager(mLayoutManager);
        if (itemDecoration != null) {
            recyclerViewBottomDoc.removeItemDecoration(itemDecoration);
        }
        itemDecoration = new ItemOffsetDecoration(context, R.dimen.dp_10);
        recyclerViewBottomDoc.addItemDecoration(itemDecoration);
        boolean isHideIconBranding = PrefSiempo.getInstance(context).read(PrefSiempo.IS_ICON_BRANDING, true);
        mAdapter = new ToolsMenuAdapter(getActivity(), isHideIconBranding, true, items);
        recyclerViewBottomDoc.setAdapter(mAdapter);

        router = new TokenRouter();
        parser = new TokenParser(router);
        mWindow = getActivity().getWindow();

        // clear FLAG_TRANSLUCENT_STATUS flag:
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        defaultStatusBarColor = mWindow.getStatusBarColor();

        //Focus Change Listener for Search in List
        searchEditTextFocusChanged();

        //Code for Date setting
        setToolsPaneDate();

        //Code for Page change
        setViewPagerPageChanged();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (router != null && searchLayout != null && searchLayout.getTxtSearchBox() != null) {
                    mediator.listItemClicked(router, position, searchLayout.getTxtSearchBox().getStrText());
                }
            }
        });

        imageClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                chipsEditText.clearFocus();
                chipsEditText.setText("");

                if (inputMethodManager != null) {
                    inputMethodManager.hideSoftInputFromWindow(chipsEditText.getWindowToken(), 0);
                }

            }
        });


    }


    public MainListAdapter getAdapter() {
        return adapter;
    }

    private void searchEditTextFocusChanged() {
        //Circular Edit Text
        edtSearchToolsRounded.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    searchLayout.setVisibility(View.VISIBLE);
                    cardViewEdtSearch.setVisibility(View.VISIBLE);
                    relSearchTools.setVisibility(View.GONE);
                    InputMethodManager imm = (InputMethodManager) getActivity()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInputFromWindow(
                            searchLayout.getApplicationWindowToken(),
                            InputMethodManager.SHOW_FORCED, 0);

                }
            }
        });


        //Listview edit Text
        chipsEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && isVisible()) {

                    linPane.setVisibility(View.GONE);
                    linBottomDoc.setVisibility(View.GONE);
                    blueLineDivider.setVisibility(View.GONE);
                    linSearchList.setVisibility(View.VISIBLE);
                    imageClear.setVisibility(View.VISIBLE);
                } else {

                    linPane.setVisibility(View.VISIBLE);
                    blueLineDivider.setVisibility(View.VISIBLE);
                    searchLayout.setVisibility(View.GONE);
                    cardViewEdtSearch.setVisibility(View.GONE);
                    relSearchTools.setVisibility(View.VISIBLE);
                    linBottomDoc.setVisibility(View.VISIBLE);
                    linSearchList.setVisibility(View.GONE);
                    imageClear.setVisibility(View.VISIBLE);

                }
            }
        });
    }


    @Subscribe
    public void searchLayoutEvent(SearchLayoutEvent event) {
        try {
            if (event.getString().equalsIgnoreCase("") || event.getString().equalsIgnoreCase("/")
                    || (event.getString().startsWith("/") && event.getString().length() == 2)) {
                listView.setAdapter(adapter);
            }
            parser.parse(event.getString());
            if (adapter != null) {
                adapter.getFilter().filter(TokenManager.getInstance().getCurrent().getTitle());
            }
        } catch (Exception e) {
            CoreApplication.getInstance().logException(e);
            Tracer.e(e, e.getMessage());
        }
    }


    @Subscribe
    public void tokenManagerEvent(TokenUpdateEvent event) {
        try {
            TokenItem current = TokenManager.getInstance().getCurrent();
            if (current != null) {
                if (current.getItemType() == TokenItemType.END_OP) {
                    mediator.defaultData();
                } else if (current.getItemType() == TokenItemType.CONTACT) {
                    if (current.getCompleteType() == TokenCompleteType.HALF) {
                        mediator.contactNumberPicker(Integer.parseInt(current.getExtra1()));
                    } else {
                        mediator.contactPicker();
                    }
                } else if (current.getItemType() == TokenItemType.DATA) {
                    if (TokenManager.getInstance().get(0).getItemType() == TokenItemType.DATA) {
                        mediator.resetData();
                        if (adapter != null)
                            adapter.getFilter().filter(current.getTitle());
                    } else {
                        mediator.resetData();
                        if (current.getTitle().trim().isEmpty()) {
                            if (adapter != null) {
                                mediator.loadDefaultData();
                                adapter.getFilter().filter("^");
                            }
                        } else {
                            if (adapter != null) {
                                mediator.loadDefaultData();
                                adapter.getFilter().filter(current.getTitle());
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
            CoreApplication.getInstance().logException(e);
            Tracer.e(e, e.getMessage());
        }
    }


    /**
     * Page Change Listener and modification of UI based on Page change
     */
    private void setViewPagerPageChanged() {
        pagerPane.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                //Make the junk food pane visible
                if (i == 0) {
                    linTopDoc.setBackgroundColor(getResources().getColor(R.color
                            .bg_junk_apps_top_dock));
                    txtTopDockDate.setVisibility(View.GONE);
                    searchLayout.setVisibility(View.GONE);
                    edtSearchToolsRounded.setVisibility(View.GONE);

                    txtIntention.setVisibility(View.VISIBLE);
                    txtIntentionLabelJunkPane.setVisibility(View.VISIBLE);

                    String strIntention = PrefSiempo.getInstance(getActivity()).read
                            (PrefSiempo.DEFAULT_INTENTION, "");
                    if (TextUtils.isEmpty(strIntention)) {
                        txtIntention.setText("You chose to hide these apps.");
                        txtIntentionLabelJunkPane.setVisibility(View.INVISIBLE);

                    } else {
                        txtIntentionLabelJunkPane.setText(getString(R.string
                                .you_ve_flag));
                        txtIntention.setText(strIntention);
                        txtIntention.setVisibility(View.VISIBLE);
                        txtIntentionLabelJunkPane.setVisibility(View.VISIBLE);
                    }


                    // finally change the color
                    mWindow.setStatusBarColor(getResources().getColor(R.color
                            .appland_blue_bright));


                }

                //Tools and Favourite Pane
                else {
                    linTopDoc.setBackground(getResources().getDrawable(R
                            .drawable.top_bar_bg));
                    txtTopDockDate.setVisibility(View.VISIBLE);
                    searchLayout.setVisibility(View.VISIBLE);
                    edtSearchToolsRounded.setVisibility(View.VISIBLE);
                    txtIntention.setVisibility(View.GONE);
                    txtIntentionLabelJunkPane.setVisibility(View.GONE);

                    // finally change the color
                    mWindow.setStatusBarColor(defaultStatusBarColor);


                }

            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }


    /**
     * Set Date for Tools Pane
     */
    private void setToolsPaneDate() {
        Calendar c = Calendar.getInstance();
        DateFormat df = getDateInstanceWithoutYears(Locale
                .getDefault());
        txtTopDockDate.setText(df.format(c.getTime()));

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.linTopDoc:
                break;
            case R.id.pagerPane:
                break;
            case R.id.linPane:
                break;
            case R.id.linBottomDoc:
                break;
        }
    }


    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        //Changing the status bar default value on page change
        if (!isVisibleToUser && null != mWindow) {
            mWindow.setStatusBarColor(defaultStatusBarColor);
        }
        if (isVisibleToUser && null != mWindow && pagerPane.getCurrentItem() == 0) {
            mWindow.setStatusBarColor(getResources().getColor(R.color
                    .appland_blue_bright));
        }
        if (!isVisibleToUser && null != imageClear && linSearchList
                .getVisibility() == View.VISIBLE) {
            imageClear.performClick();
        }

        super.setUserVisibleHint(isVisibleToUser);
    }

    public DateFormat getDateInstanceWithoutYears(Locale locale) {
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance
                (DateFormat.FULL, locale);
        sdf.applyPattern(sdf.toPattern().replaceAll("[^\\p{Alpha}]*y+[^\\p{Alpha}]*", ""));
        return sdf;
    }


    public TokenManager getManager() {
        return TokenManager.getInstance();
    }

    @Override
    public void onPause() {

        super.onPause();
        if (null != mWindow) {
            mWindow.setStatusBarColor(defaultStatusBarColor);
        }
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(chipsEditText.getWindowToken(), 0);
        }
    }

    public void resetSearchList() {
        parser.parse("");
        if (adapter != null) {
            adapter.getFilter().filter("");
        }
    }


    public void setCurrentPage(int viewPagerPage) {
        pagerPane.setCurrentItem(viewPagerPage);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(chipsEditText.getWindowToken(), 0);
        }
    }
}
