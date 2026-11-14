package ai.pleos.smoke;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import ai.pleos.smoke.core.Tariff;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SettingsActivity extends UiActivity {
    private EditText base,meters,step,unit,seconds,low,outside,round,baseRound;
    private Spinner stacking,character,theme,region;
    private Switch automatic,night;
    private LinearLayout bands;
    private final List<EditText[]> rows=new ArrayList<>();
    public void onCreate(Bundle state){super.onCreate(state);build();}
    private EditText field(LinearLayout parent,String title,String value){
        parent.addView(text(title,15));EditText e=new EditText(this);e.setSingleLine(true);e.setText(value);e.setTextColor(ink);
        e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);parent.addView(e);return e;
    }
    private Spinner select(LinearLayout parent,String title,String[] labels,int selection){parent.addView(text(title,15));Spinner s=new Spinner(this);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels);s.setAdapter(adapter);s.setSelection(selection);parent.addView(s);return s;}
    private void build(){
        Tariff t=preferences.custom();LinearLayout l=column();l.addView(text("설정",26));l.addView(button("미터로 돌아가기",this::finish));
        region=select(l,"요금 지역",new String[]{"서울 · 중형 일반","직접 설정"},preferences.isCustom()?1:0);
        TextView preset=text("서울시 공식 안내 · 2023-02-01 적용 기준\n4,800원 / 1.6km · 131m 또는 저속 30초당 100원\n심야 20·40·20% / 시외 20% / 중복 합산 최대 60%",15);l.addView(preset);
        LinearLayout editor=column();l.addView(editor);LinearLayout outer=l;l=editor;
        base=field(l,"기본요금 (원)",""+t.baseWon);meters=field(l,"기본거리 (m)",""+t.baseMeters);
        step=field(l,"추가요금 단위 (원)",""+t.stepWon);unit=field(l,"추가거리 단위 (m)",""+t.distanceUnitMeters);
        seconds=field(l,"저속 시간 단위 (초)",""+(t.timeUnitMillis/1000.0));low=field(l,"저속 기준 (km/h)",""+t.lowSpeedKmh);
        round=field(l,"요금 반올림 단위 (원)",""+t.roundingWon);
        baseRound=field(l,"기본요금 할증 반올림 단위 (원)",""+t.baseRoundingWon);
        night=new Switch(this);night.setText("심야할증 자동 적용");night.setTextColor(ink);night.setChecked(!t.nightBands.isEmpty());l.addView(night);
        l.addView(text("심야 시간대 · 시작~종료 / 할증률",18));bands=new LinearLayout(this);bands.setOrientation(1);l.addView(bands);
        for(var b:t.nightBands)addBand(b.startMinute(),b.endMinute(),b.percent());
        l.addView(button("시간대 추가",()->addBand(1320,1380,20)));
        outside=field(l,"시외할증률 (%)",""+t.outsidePercent);
        stacking=select(l,"심야·시외 중복 적용",new String[]{"할증률 합산","할증률 곱셈","더 높은 할증만"},t.stacking.ordinal());
        l=outer;editor.setVisibility(region.getSelectedItemPosition()==1?android.view.View.VISIBLE:android.view.View.GONE);preset.setVisibility(region.getSelectedItemPosition()==0?android.view.View.VISIBLE:android.view.View.GONE);
        region.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){editor.setVisibility(pos==1?android.view.View.VISIBLE:android.view.View.GONE);preset.setVisibility(pos==0?android.view.View.VISIBLE:android.view.View.GONE);}});
        automatic=new Switch(this);automatic.setText("행정구역 변경 시 시외할증 자동 적용");automatic.setTextColor(ink);automatic.setChecked(preferences.automatic());l.addView(automatic);
        l.addView(text("메인에서 시외할증을 켜거나 끄면 수동 모드로 바뀝니다.",14));
        character=select(l,"캐릭터",new String[]{"말","고양이","토끼","오리"},List.of("horse","cat","rabbit","duck").indexOf(preferences.character()));
        theme=select(l,"화면 모드",new String[]{"차량 주야간 연동","주간","야간"},List.of("auto","day","night").indexOf(preferences.theme()));
        l.addView(button("저장",()->save()));l.addView(text("진행 중인 운행의 요금제는 유지됩니다. 요금표 변경은 다음 운행부터 적용됩니다.",14));scroll(l);
    }
    private void addBand(int start,int end,int percent){LinearLayout row=new LinearLayout(this);row.setOrientation(0);
        EditText a=new EditText(this),b=new EditText(this),c=new EditText(this);EditText[] values={a,b,c};
        a.setText(clock(start));b.setText(clock(end));c.setText(""+percent);
        for(int i=0;i<3;i++){EditText e=values[i];e.setTextColor(ink);e.setSingleLine(true);e.setTextSize(16);e.setInputType(i==2?InputType.TYPE_CLASS_NUMBER:InputType.TYPE_CLASS_DATETIME|InputType.TYPE_DATETIME_VARIATION_TIME);e.setContentDescription(i==0?"시작 시각 HH:mm":i==1?"종료 시각 HH:mm":"할증률 퍼센트");row.addView(e,new LinearLayout.LayoutParams(0,dp(52),1));}
        row.addView(button("삭제",()->{bands.removeView(row);rows.remove(values);}));rows.add(values);bands.addView(row);
    }
    private String clock(int m){return String.format(Locale.ROOT,"%02d:%02d",m/60,m%60);}
    private int minute(String value){if(!value.matches("[0-2][0-9]:[0-5][0-9]"))throw new IllegalArgumentException("시각은 HH:mm 형식입니다");int h=Integer.parseInt(value.substring(0,2));if(h>23)throw new IllegalArgumentException("시각은 00:00~23:59입니다");return h*60+Integer.parseInt(value.substring(3));}
    private long num(EditText e){return Long.parseLong(e.getText().toString());}
    private double decimal(EditText e){return Double.parseDouble(e.getText().toString());}
    private void save(){try{
        boolean custom=region.getSelectedItemPosition()==1;
        if(custom){
        List<Tariff.NightBand> list=new ArrayList<>();if(night.isChecked())for(EditText[] r:rows)list.add(new Tariff.NightBand(minute(r[0].getText().toString()),minute(r[1].getText().toString()),(int)num(r[2])));
        if(night.isChecked()&&list.isEmpty())throw new IllegalArgumentException("심야 시간대를 추가하세요");
        long time=Math.round(decimal(seconds)*1000);long outsideRate=num(outside);if(outsideRate>200)throw new IllegalArgumentException("시외할증률을 확인하세요");
        Tariff tariff=new Tariff("직접 설정","custom-"+System.currentTimeMillis(),"사용자 직접 설정","",num(base),decimal(meters),num(step),decimal(unit),time,decimal(low),(int)outsideRate,
                Tariff.Stacking.values()[stacking.getSelectedItemPosition()],list,num(round),num(baseRound),"Asia/Seoul");
        preferences.saveCustom(tariff);}
        preferences.selectCustom(custom);preferences.setOutside(automatic.isChecked(),preferences.applied());
        preferences.setAppearance(new String[]{"horse","cat","rabbit","duck"}[character.getSelectedItemPosition()],new String[]{"auto","day","night"}[theme.getSelectedItemPosition()]);
        Toast.makeText(this,"저장했습니다",Toast.LENGTH_SHORT).show();finish();
    }catch(Exception e){new android.app.AlertDialog.Builder(this).setMessage(e.getMessage()==null?"입력값을 확인하세요":e.getMessage()).setPositiveButton("확인",null).show();}}
}
