package com.example.victor.penumbra;

        import java.util.ArrayList;
        import java.util.List;

        import android.app.Activity;
        import android.content.Intent;
        import android.os.Bundle;
        import android.view.View;
        import android.widget.AdapterView;
        import android.widget.AdapterView.OnItemClickListener;
        import android.widget.ArrayAdapter;
        import android.widget.ListView;

public class MainActivity extends Activity {

    private ListView lv;

    private static final String OPCAO1 = "Novo Jogo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		/*
		 * Simples lista que corresponde
		 * aos dados que queremos exibir
		 * em forma de lista
		 */
        List<String> itens = new ArrayList<String>();

        itens.add(OPCAO1);

		/*
		 * ArrayAdapter determina como os dados
		 * serao exibidos no componente de UI.
		 */
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, itens);

		/*
		 * listView1 que se encontra em activity_main.xml
		 * eh o componente de UI que vai exibir os dados
		 * em forma de lista
		 */
        lv = (ListView) findViewById(R.id.listView1);

		/*
		 * Associa ArrayAdapter e ListView
		 */
        lv.setAdapter(arrayAdapter);

        //continua
        //continuacao
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, int position,
                                    long arg3) {

                String opcao = (String) av.getItemAtPosition(position);
                if (OPCAO1.equals(opcao)) {
                    startLinearLayoutDemoActivity();

                }
            }
        });
    }

    public void startLinearLayoutDemoActivity() {
        startActivity(new Intent(this, Game.class));
    }

}
