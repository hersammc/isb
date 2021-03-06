package com.postech.isb.readThread;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import com.postech.isb.PostechIsb;
import com.postech.isb.R;
import com.postech.isb.R.id;
import com.postech.isb.R.layout;
import com.postech.isb.boardList.Board;
import com.postech.isb.boardList.BoardList;
import com.postech.isb.boardList.IsbDBAdapter;
import com.postech.isb.compose.NotePad.Notes;
import com.postech.isb.util.IsbSession;
import com.postech.isb.util.IsbThread;
import com.postech.isb.util.ThreadList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.widget.Toast;
import android.widget.TextView.BufferType;

public class ReadThread extends Activity {

	private IsbSession isb;
	private String board;
	private int num;

	private TextView boardName;
	private Button prev;
	private Button next;
	private TextView threadHead;
	private TextView threadBody;
	private TableLayout comments;

	private TextView commentMessage;
	private Button leaveCommentBtn;
	private ScrollView readThreadScroll;

	private LinearLayout linearLayout;
	private LinearLayout linearLayoutInner;

	static final private int WRITE = Menu.FIRST;
	static final private int DELETE = Menu.FIRST + 1;

	/**
	 * Standard projection for the interesting columns of a normal note.
	 */
	private static final String[] PROJECTION = new String[] { Notes._ID, // 0
			Notes.TITLE, // 1
			Notes.NOTE, // 2
			Notes.TARGET_BOARD, // 3
	};
	/** The index of the note column */
	private static final int COLUMN_INDEX_TITLE = 1;
	private static final int COLUMN_INDEX_NOTE = 2;
	private static final int COLUMN_INDEX_TARGET_BOARD = 3;

	static final private int NewNote = 1;

	private Intent gotoAnotherBoard;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		Intent intent = getIntent();
		board = intent.getStringExtra("board");
		num = intent.getIntExtra("num", -1);

		isb = ((PostechIsb) getApplicationContext()).isb;

		if (board == null) {
			Toast.makeText(getApplicationContext(), "Board is missing",
					Toast.LENGTH_SHORT).show();
			finish();
		}

		if (num == -1) {
			Toast.makeText(getApplicationContext(), "Thread number is missing",
					Toast.LENGTH_SHORT).show();
			finish();
		}

		setContentView(R.layout.read_thread);

		boardName = (TextView) findViewById(R.id.curBoardinReadThead);
		threadHead = (TextView) findViewById(R.id.threadHead);
		threadBody = (TextView) findViewById(R.id.threadBody);
		comments = (TableLayout) findViewById(R.id.threadComment);

		commentMessage = (TextView) findViewById(R.id.commentText);
		leaveCommentBtn = (Button) findViewById(R.id.commentBtn);

		prev = (Button) findViewById(R.id.prevThread);
		next = (Button) findViewById(R.id.nextThread);

		readThreadScroll = (ScrollView) findViewById(R.id.readThreadScroll);

		boardName.setText(board);

		gotoAnotherBoard = new Intent(this, BoardList.class);
		gotoAnotherBoard.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

		boardName.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				startActivity(gotoAnotherBoard);
				finish();
			}
		});

		prev.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateThread(num - 1);
			}
		});

		next.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				updateThread(num + 1);
			}
		});

		leaveCommentBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String message = commentMessage.getText().toString();

				if (message.length() > 0) {
					try {
						if (isb.isMain()) {
							leaveCommentBtn.setVisibility(ImageButton.GONE);
							if (isb.leaveComment(board, num, message)) {
								updateThread(num);
								commentMessage.setText("");
							} else
								Toast.makeText(getApplicationContext(),
										"Unexpected error.T.T",
										Toast.LENGTH_SHORT).show();
							leaveCommentBtn.setVisibility(ImageButton.VISIBLE);

						} else {
							Toast.makeText(getApplicationContext(),
									"Login first plz...", Toast.LENGTH_SHORT)
									.show();
						}
					} catch (IOException e) {
						Toast.makeText(getApplicationContext(),
								"Connection lost!", Toast.LENGTH_SHORT).show();
					}
				}
			}
		});

		linearLayout = (LinearLayout) findViewById(R.id.linearLayout);
		linearLayout.setOnTouchListener(MyTouchListener);
		readThreadScroll.setOnTouchListener(MyTouchListener);
		linearLayoutInner = (LinearLayout) findViewById(R.id.linearLayoutInsideScroll);
		linearLayoutInner.setOnTouchListener(MyTouchListener);	
		threadBody.setOnTouchListener(MyTouchListener);

		registerForContextMenu(boardName);
		updateThread(num);
	}

	public class ClickableSpanLink extends ClickableSpan {
		String m_str;

		public ClickableSpanLink(String _str) {
			m_str = _str;
		}

		@Override
		public void onClick(View view) {
			boolean fValidFormat = true;
			try {
				Log.i("debug", "onClick : " + m_str);
				String strNum = m_str.substring(m_str.indexOf('/') + 3);
				int _num = Integer.parseInt(strNum.trim());
				num = _num;
			} catch (NumberFormatException e) {
				fValidFormat = false;
			}
			int loc = m_str.indexOf('/');

			String strShortBoard = Character.toString(m_str.charAt(loc - 1))
					+ m_str.charAt(loc + 1);
			board = getBoardNameByInitial(strShortBoard);
			if (board.length() <= 0)
				fValidFormat = false;

			if (fValidFormat)
				updateThread(num);
		}

		public String getBoardNameByInitial(String initial) {
			String boardname = "";
			ArrayList<Board> boardList;
			try {
				boardList = isb.getBoardList();
				Iterator<Board> iter = boardList.iterator();
				while (iter.hasNext()) {
					Board board = iter.next();
					String name = board.name;
					if (name.charAt(0) != initial.charAt(0))
						continue;
					int idx2 = name.indexOf('/') + 1;
					if (name.charAt(idx2) == initial.charAt(1)) {
						boardname = name;
						break;
					}

				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return boardname;
		}
	}

	private void addRefLink(TextView textview, String str) {
		String strThreadBody = str;
		SpannableString text = new SpannableString(strThreadBody);
		textview.setText(text);

		// Pattern pattern =
		// Pattern.compile("\\s[a-zA-z]/[a-zA-z]\\s\\d{1,}\\s");
		Pattern linkPattern = Pattern
				.compile("(\\s|^)[a-zA-z0-9��-�R]/[a-zA-z0-9��-�R]\\s\\d{1,}(\\s|$)");
		Matcher match = linkPattern.matcher(strThreadBody);
		final Context context = this;
		while (match.find()) { // Find each match in turn; String can't do this.
			ClickableSpan clickableSpan = new ClickableSpanLink(match.group());
			text.setSpan(clickableSpan, match.start(), match.end(), 0);
		}
		textview.setMovementMethod(LinkMovementMethod.getInstance());
		textview.setText(text, BufferType.SPANNABLE);

	}

	private boolean isUrl(String str) {
		boolean f = true;
		try {
			URL url = new URL(str);
		} catch (MalformedURLException e) {
			f = false;// it wasn't a URL
		}
		return f;
	}

	private String preprocessLink(String str) {
		StringBuffer result = new StringBuffer();
		String regUrl = "https?://([\\w-]+\\.)+[\\w-]+(/[\\w-./?&%=]*)?";
		String regUrlTail = "[a-zA-Z0-9\\-\\._\\?\\,\\'/\\\\+&amp;%\\$#\\=~]*";
		String delimiter = "\n";
		String[] astr = str.split(delimiter);

		boolean fReadingUrl = true;
		boolean fAddLF = true;
		for (int i = 0; i < astr.length; i++) {
			// Log.i("clover", String.format("%d : ", i) + astr[i] );
			if (isUrl(astr[i]) && astr[i].length() >= 79)
				fReadingUrl = true;
			else if (fReadingUrl && astr[i].matches(regUrlTail)) {
				fAddLF = false;
				if (astr[i].length() < 79)
					fReadingUrl = false;
			} else
				fReadingUrl = false;

			// Log.i("clover", String.format("fReadingUrl= %b , fAddLF=%b ",
			// fReadingUrl, fAddLF) );

			if (fAddLF)
				result.append(delimiter);
			result.append(astr[i]);
			if (!fReadingUrl)
				fAddLF = true;
		}
		result.append(delimiter);

		return result.toString();
	}

	static String TrimLine(String str) {

		int lastNewline = -1;
		int i;
		for (i = 0; i < str.length(); i++) {
			if (str.charAt(i) == '\n')
				lastNewline = i;
			else if (str.charAt(i) != '\n' && str.charAt(i) != ' '
					&& str.charAt(i) != '\f' && str.charAt(i) != '\r'
					&& str.charAt(i) != '\b' && str.charAt(i) != '\t')
				break;
		}
		if (lastNewline >= 0 && i < str.length() && lastNewline < str.length()) {
			str = str.substring(lastNewline + 1);
		}

		int lastChar = -1;
		for (i = 0; i < str.length(); i++) {
			if (str.charAt(i) != '\n' && str.charAt(i) != ' '
					&& str.charAt(i) != '\f' && str.charAt(i) != '\r'
					&& str.charAt(i) != '\b' && str.charAt(i) != '\t')
				lastChar = i;
		}
		if (lastChar >= 0 && lastChar < str.length()) {
			str = str.substring(0, lastChar + 1);
		}
		return str;
	}

	private boolean updateThread(int _num) {
		try {
			if (isb.isMain()) {
				boolean readNext = (_num >= num ); 
				if( readNext )
					linearLayout.setAnimation(AnimationUtils.loadAnimation(ReadThread.this, R.anim.slide_out_left));
				else
					linearLayout.setAnimation(AnimationUtils.loadAnimation(ReadThread.this, android.R.anim.slide_out_right));
				IsbThread t = isb.readThread(board, _num);
				
				if (t != null) {

					boardName.setText(board + " (" + _num + ")");
					threadHead.setText(t.writer + "\n" + t.date + "\n"
							+ t.title);
					Log.i("debug", "write : " + t.writer);

					// // link ref maker
					String strContent = preprocessLink(TrimLine(t.contents));
					addRefLink(threadBody, strContent);

					Linkify.addLinks(threadBody, Linkify.WEB_URLS);

					// t.comments
					Scanner s = new Scanner(t.comments);
					s.useDelimiter("\n");

					Log.i("debug", "ReadThread : " + t.comments);

					Pattern p = Pattern
							.compile("^\\s*(\\w+)\\((\\d+,\\d+:\\d+)\\):\"(.*)\"\n?$");

					TableRow row;
					TextView writer, comment, when;

					comments.removeAllViews();

					while (s.hasNext()) {
						String token = s.next();
						Matcher m = p.matcher(token);

						if (m.matches()) {
							row = new TableRow(this);
							row.setLayoutParams(new LayoutParams(
									LayoutParams.FILL_PARENT,
									LayoutParams.WRAP_CONTENT));

							writer = new TextView(this);
							writer.setText(m.replaceAll("$1"));
							writer.setPadding(0, 0, 5, 0);
							writer.setTextColor(0xFFCCCCFF);
							writer.setTextSize(15);

							when = new TextView(this);
							/*
							 * when.setText(m.replaceAll("$2"));
							 * when.setPadding(0, 0, 5, 0);
							 */

							comment = new TextView(this);

							// comment.setText(m.replaceAll("$3"));
							addRefLink(comment, m.replaceAll("$3"));
							comment.setTextColor(0xFFFFFFFF);
							comment.setTextSize(15);

							row.addView(writer);
							row.addView(when);
							row.addView(comment);

							comments.addView(row);
						} else
							Log.i("debug", "ReadThread unmatch : " + token);
					}

					num = _num;
					prev.setVisibility(num == 1 ? View.INVISIBLE : View.VISIBLE);
					next.setVisibility(View.VISIBLE);
					readThreadScroll.smoothScrollTo(0, 0);
					
					return true;
				} else {
					Toast.makeText(getApplicationContext(), "No more thread!",
							Toast.LENGTH_SHORT).show();
					// next.setVisibility(View.INVISIBLE);
					finish();
				}
			} else {
				Toast.makeText(getApplicationContext(), "Login first plz...",
						Toast.LENGTH_SHORT).show();
			}
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), "Connection lost!",
					Toast.LENGTH_SHORT).show();
		}

		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		menu.add(0, WRITE, Menu.NONE, R.string.write).setShortcut('3', 'w')
				.setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, DELETE, Menu.NONE, R.string.delete).setShortcut('4', 'd')
				.setIcon(android.R.drawable.ic_menu_delete);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case WRITE: {
			Intent giveMeNewThread = new Intent(Intent.ACTION_INSERT,
					Notes.CONTENT_URI);
			giveMeNewThread.putExtra("board", board);
			startActivityForResult(giveMeNewThread, NewNote);
			return true;
		}
		case DELETE: {
			if (isb.isMain()) {
//				try {
					AlertDialog.Builder alert_confirm = new AlertDialog.Builder(ReadThread.this);
					alert_confirm.setMessage(R.string.delete_confirm).setCancelable(false).setPositiveButton(R.string.delete,
					new DialogInterface.OnClickListener() {
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
					        // 'YES'
					    	try{
								if (isb.modifyThread(board, num, isb.THREAD_DELETE)) {
									Toast.makeText(getApplicationContext(),
											"Delete success", Toast.LENGTH_SHORT).show();
									finish();
								} else {
									Toast.makeText(getApplicationContext(), "Delete fail.",
											Toast.LENGTH_SHORT).show();
								}					}
							catch (IOException e) {
								e.printStackTrace();
								Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
								isb.disconnect();
							}
					    }
					}).setNegativeButton(R.string.cancle,
					new DialogInterface.OnClickListener() {
					    @Override
					    public void onClick(DialogInterface dialog, int which) {
					        // 'No'
					    return;
					    }
					});
					AlertDialog alert = alert_confirm.create();
					alert.show();

			} else
				Toast.makeText(getApplicationContext(), "Login first plz...",
						Toast.LENGTH_SHORT).show();

			return true;
		}
		}

		return false;
	}

	private int m_nPreTouchPosY = 0;
	private int m_nPreTouchPosX = 0;
	View.OnTouchListener MyTouchListener = new View.OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				m_nPreTouchPosX = (int) event.getX();
				m_nPreTouchPosY = (int) event.getY();
			}

			if (event.getAction() == MotionEvent.ACTION_UP) {
				int nTouchPosX = (int) event.getX();
				int nTouchPosY = (int) event.getY();
				
				Display display = getWindowManager().getDefaultDisplay();
				int width = display.getWidth();

				Log.i("clover", "x : " + m_nPreTouchPosX + " -> " + nTouchPosX);
				Log.i("clover", "Width = " + width );
				
				if( Math.abs(nTouchPosX - m_nPreTouchPosX) < 4 
				 && Math.abs(nTouchPosY - m_nPreTouchPosY) < 4  )
				{
					// Do Nothing 
				}
				else
				{
					int gap = width / 5;
					if ( nTouchPosX - m_nPreTouchPosX < -gap && m_nPreTouchPosX > width * (3./4) ) 
					{
						updateThread(num + 1);
					} 
					else if (nTouchPosX - m_nPreTouchPosX > gap && m_nPreTouchPosX < width / 4. ) 
					{
						updateThread(num - 1);
					}
				}
				m_nPreTouchPosX = nTouchPosX;
				m_nPreTouchPosY = nTouchPosY;
			}
			return false;
		}
	};
	
	class MyScrollView extends ScrollView{
		public MyScrollView(Context context) {
			super(context);
		}
		//��ũ�Ѻ� �̺�Ʈ ó���κ�
		public boolean onInterceptTouchEvent(MotionEvent ev){
			int action = ev.getAction();
			switch (action)
			{
				case MotionEvent.ACTION_DOWN:
					// Disallow ScrollView to intercept touch events.
//					this.getParent().requestDisallowInterceptTouchEvent(true);
					break;

				case MotionEvent.ACTION_UP:
					// Allow ScrollView to intercept touch events.
//					this.getParent().requestDisallowInterceptTouchEvent(false);
					break;
			}
			super.onTouchEvent(ev);
			return true;
		}
	}

	@Override
	public void onActivityResult(int reqCode, int resCode, Intent data) {
		super.onActivityResult(reqCode, resCode, data);

		switch (reqCode) {
		case (NewNote): {
			if (resCode == Activity.RESULT_OK) {
				String result = data.getAction();
				Uri resultUri = Uri.parse(result);

				Cursor cursor = managedQuery(resultUri, PROJECTION, null, null,
						Notes.DEFAULT_SORT_ORDER);

				if (cursor.moveToFirst()) {
					String title = cursor.getString(COLUMN_INDEX_TITLE);
					String content = cursor.getString(COLUMN_INDEX_NOTE);
					String targetBoard = cursor
							.getString(COLUMN_INDEX_TARGET_BOARD);

					if (isb.isMain()) {
						try {
							if (isb.writeToBoard(targetBoard, title, content)) {
								Toast.makeText(getApplicationContext(),
										"Write success", Toast.LENGTH_SHORT)
										.show();
								ContentResolver cr = getContentResolver();
								if (cr.delete(resultUri, null, null) > 0) {
									Log.i("debug", "Delete success");
								}
							} else {
								Toast.makeText(getApplicationContext(),
										"Fail! Invalid board?",
										Toast.LENGTH_SHORT).show();
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							Toast.makeText(getApplicationContext(),
									"Connection lost", Toast.LENGTH_SHORT)
									.show();
							isb.disconnect();
						}
					} else
						Toast.makeText(getApplicationContext(),
								"Login first plz...", Toast.LENGTH_SHORT)
								.show();
				}
			}
			finish();
			break;
		}
		}
	}

}
