package com.chaigene.petnolja.ui.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.applikeysolutions.cosmocalendar.model.Day;
import com.applikeysolutions.cosmocalendar.model.Month;
import com.applikeysolutions.cosmocalendar.selection.BaseSelectionManager;
import com.applikeysolutions.cosmocalendar.selection.MultipleSelectionManager;
import com.applikeysolutions.cosmocalendar.selection.RangeSelectionManager;
import com.applikeysolutions.cosmocalendar.selection.criteria.BaseCriteria;
import com.applikeysolutions.cosmocalendar.selection.criteria.WeekDayCriteria;
import com.applikeysolutions.cosmocalendar.utils.SelectionType;
import com.applikeysolutions.cosmocalendar.view.CalendarView;
import com.chaigene.petnolja.R;
import com.chaigene.petnolja.manager.FirestoreManager;
import com.chaigene.petnolja.manager.TasksManager;
import com.chaigene.petnolja.model.Quota;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import static com.chaigene.petnolja.model.Quota.FIELD_DATE;
import static com.chaigene.petnolja.model.Quota.FIELD_MONTH;
import static com.chaigene.petnolja.model.Quota.FIELD_SHOP_ID;
import static com.chaigene.petnolja.model.Quota.FIELD_YEAR;

public class CalendarActivity extends BaseActivity {
    private static final String TAG = "DefaultCalendarActivity";

    private CalendarView calendarView;

    private List<BaseCriteria> threeMonthsCriteriaList;
    private WeekDayCriteria fridayCriteria;

    private boolean fridayCriteriaEnabled;
    private boolean threeMonthsCriteriaEnabled;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setTitle("예약하기");

        initViews();
    }

    private void initViews() {
        calendarView = findViewById(R.id.calendar_view);
        calendarView.setSelectionType(SelectionType.RANGE);

        // TODO: 매번 달이 바뀔 때마다 Quota를 대입해주는 것은 문제가 있다.
        // flag를 두고 해당 달에 대한 Quota 정보가 서버로부터 받아져왔는지를
        // 기준으로 삽입해주는 것이 좋을 것 같다.

        calendarView.setOnMonthChangeListener(month -> {
            Log.i(TAG, "onMonthChanged:month:" + month);
            if (!month.isSynced()) updateQuotas(month);
        });

        calendarView.setOnDaysSelectionListener(selectedDays -> {
            Log.i(TAG, "onDaysSelected:selectedDays:" + selectedDays);
            BaseSelectionManager selectionManager = calendarView.getSelectionManager();
            if (selectionManager instanceof RangeSelectionManager) {
                Pair<Day, Day> days = ((RangeSelectionManager) selectionManager).getDays();
                if (days != null) {
                    Log.i(TAG, "selectedDays:" + selectedDays);
                }
            }
        });

        Month currentMonth = calendarView.getInitialMonth();
        updateQuotas(currentMonth);
    }

    private void updateQuotas(Month month) {
        final int MAX_QUOTA = 10;
        showLoadingDialog();
        TasksManager.call(() -> {
            Calendar calendar = month.getFirstDay().getCalendar();
            int selectedYear = calendar.get(Calendar.YEAR);
            int selectedMonth = calendar.get(Calendar.MONTH) + 1;
            Task<List<Quota>> quotasTask = CalendarActivity.this.loadQuotas(selectedYear, selectedMonth);
            List<Quota> quotas = Tasks.await(quotasTask);
            return quotas;
        }).addOnSuccessListener(quotas -> {
            dismissDialog();
            List<Day> currentMonthDays = month.getDays();
            for (Day day : currentMonthDays) {
                Quota quota = CalendarActivity.this.getQuotaByDate(quotas, day.getDayNumber());
                int occupied = quota != null ? quota.getOccupied() : 0;

                day.setMaxQuata(MAX_QUOTA);
                day.setOccupied(occupied);
            }
            month.setSynced(true);
            calendarView.update();
        }).addOnFailureListener(e -> {
            dismissDialog();
        });
    }

    private Quota getQuotaByDate(List<Quota> quotas, int date) {
        for (Quota quota : quotas) if (quota.getDate() == date) return quota;
        return null;
    }

    private Task<List<Quota>> loadQuotas(int year, int month) {
        return TasksManager.call(() -> {
            Query quotasRef = FirestoreManager.getHotelBookingQuotasRef()
                    .whereEqualTo(FIELD_SHOP_ID, "maone")
                    .whereEqualTo(FIELD_YEAR, year)
                    .whereEqualTo(FIELD_MONTH, month)
                    .orderBy(FIELD_DATE, Query.Direction.ASCENDING);
            List<Quota> quotas = Tasks.await(FirestoreManager.getInstance().get(quotasRef, Quota.class));
            return quotas;
        });
    }

    @Deprecated
    private void createCriterias() {
        // fridayCriteria = new WeekDayCriteria(Calendar.FRIDAY);

        // threeMonthsCriteriaList = new ArrayList<>();
        // threeMonthsCriteriaList.add(new CurrentMonthCriteria());
        // threeMonthsCriteriaList.add(new NextMonthCriteria());
        // threeMonthsCriteriaList.add(new PreviousMonthCriteria());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity_calendar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear_selections:
                clearSelectionsMenuClick();
                return true;

            case R.id.log_selected_days:
                logSelectedDaysMenuClick();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Deprecated
    private void selectAllFridays() {
        if (calendarView.getSelectionManager() instanceof MultipleSelectionManager) {
            ((MultipleSelectionManager) calendarView.getSelectionManager()).addCriteria(fridayCriteria);
        }
        calendarView.update();
    }

    @Deprecated
    private void unselectAllFridays() {
        if (calendarView.getSelectionManager() instanceof MultipleSelectionManager) {
            ((MultipleSelectionManager) calendarView.getSelectionManager()).removeCriteria(fridayCriteria);
        }
        calendarView.update();
    }

    @Deprecated
    private void selectThreeMonths() {
        if (calendarView.getSelectionManager() instanceof MultipleSelectionManager) {
            ((MultipleSelectionManager) calendarView.getSelectionManager()).addCriteriaList(threeMonthsCriteriaList);
        }
        calendarView.update();
    }

    @Deprecated
    private void unselectThreeMonths() {
        if (calendarView.getSelectionManager() instanceof MultipleSelectionManager) {
            ((MultipleSelectionManager) calendarView.getSelectionManager()).removeCriteriaList(threeMonthsCriteriaList);
        }
        calendarView.update();
    }

    private void clearSelectionsMenuClick() {
        calendarView.clearSelections();

        fridayCriteriaEnabled = false;
        threeMonthsCriteriaEnabled = false;
    }

    private void logSelectedDaysMenuClick() {
        Toast.makeText(this, "Selected " + calendarView.getSelectedDays().size(), Toast.LENGTH_SHORT).show();
    }
}