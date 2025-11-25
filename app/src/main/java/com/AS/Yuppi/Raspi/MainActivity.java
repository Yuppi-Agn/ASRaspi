package com.AS.Yuppi.Raspi;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.AS.Yuppi.Raspi.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_schedule_redactor2,
                R.id.nav_schedule_redactor,
                R.id.nav_hometaskRedactor)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        setupCustomNavigation(navController);
        //ЛОГИКА ПОДСВЕТКИ АКТИВНОГО ПУНКТА МЕНЮ
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            View navView = findViewById(R.id.custom_nav_view);
            if (navView == null) return;

            // Сбрасываем подсветку со всех пунктов
            navView.findViewById(R.id.nav_home).setActivated(false);
            navView.findViewById(R.id.nav_schedule_redactor).setActivated(false);
            navView.findViewById(R.id.nav_schedule_redactor2).setActivated(false);
            navView.findViewById(R.id.nav_hometaskRedactor).setActivated(false);

            // Включаем подсветку для текущего пункта
            int currentDestinationId = destination.getId();
            View currentView = navView.findViewById(currentDestinationId);
            if (currentView != null) {
                currentView.setActivated(true);
            }
        });
    }
    private void setupCustomNavigation(NavController navController) {
        // Находим родительский View нашего кастомного меню
        View navView = findViewById(R.id.custom_nav_view);
        DrawerLayout drawer = findViewById(R.id.drawer_layout); // Находим DrawerLayout для закрытия

        // --- Пункт 1: Главная ---
        View navHome = navView.findViewById(R.id.nav_home);
        TextView titleHome = navHome.findViewById(R.id.item_title);
        ImageView iconHome = navHome.findViewById(R.id.item_icon);
        titleHome.setText(R.string.menu_home);
        iconHome.setImageResource(R.drawable.ic_menu_camera);
        navHome.setOnClickListener(v -> {
            navController.navigate(R.id.nav_home); // <--- ИСПОЛЬЗУЕМ NAVCONTROLLER
            drawer.closeDrawer(GravityCompat.START); // Закрываем шторку
        });

        // --- Пункт 2: Редактор расписаний ---
        View navSchedule = navView.findViewById(R.id.nav_schedule_redactor);
        TextView titleSchedule = navSchedule.findViewById(R.id.item_title);
        ImageView iconSchedule = navSchedule.findViewById(R.id.item_icon);
        titleSchedule.setText(R.string.menu_ScheduleRedactor);
        iconSchedule.setImageResource(R.drawable.assignment_24);
        navSchedule.setOnClickListener(v -> {
            navController.navigate(R.id.nav_schedule_redactor); // <--- ИСПОЛЬЗУЕМ NAVCONTROLLER
            drawer.closeDrawer(GravityCompat.START);
        });

        // --- Пункт 3: Старый редактор ---
        View navSchedule2 = navView.findViewById(R.id.nav_schedule_redactor2);
        TextView titleSchedule2 = navSchedule2.findViewById(R.id.item_title);
        ImageView iconSchedule2 = navSchedule2.findViewById(R.id.item_icon);
        titleSchedule2.setText("Старый редактор");
        iconSchedule2.setImageResource(R.drawable.assignment_24);
        navSchedule2.setOnClickListener(v -> {
            navController.navigate(R.id.nav_schedule_redactor2); // <--- ИСПОЛЬЗУЕМ NAVCONTROLLER
            drawer.closeDrawer(GravityCompat.START);
        });

        // --- Пункт 4: Задания и задачи ---
        View navHometask = navView.findViewById(R.id.nav_hometaskRedactor);
        TextView titleHometask = navHometask.findViewById(R.id.item_title);
        ImageView iconHometask = navHometask.findViewById(R.id.item_icon);
        titleHometask.setText(R.string.menu_HometaskRedactor);
        iconHometask.setImageResource(R.drawable.edit_24);
        navHometask.setOnClickListener(v -> {
            // Убедитесь, что у вас есть action или destination с этим ID в nav_graph
            navController.navigate(R.id.nav_hometaskRedactor); // <--- ИСПОЛЬЗУЕМ NAVCONTROLLER
            drawer.closeDrawer(GravityCompat.START);
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}