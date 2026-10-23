package studio.jiayu.neoagent;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Non-exported. Android grants the installer access to exactly one verified, read-only URI. */
public final class UpdateApkProvider extends ContentProvider {
 public boolean onCreate(){return true;}
 private File file(Uri uri)throws FileNotFoundException{try{return AppUpdateStore.verified(getContext(),uri);}catch(Exception rejected){throw new FileNotFoundException("更新文件不可用或校验未通过");}}
 public String getType(Uri uri){if(!uri.equals(AppUpdateStore.uri(getContext())))throw new IllegalArgumentException("不支持的更新地址");return "application/vnd.android.package-archive";}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("更新文件只读");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder){File f;try{f=file(uri);}catch(FileNotFoundException e){return null;}String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor result=new MatrixCursor(cols);Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(cols[i])?"Neo-Agent-Touch-update.apk":OpenableColumns.SIZE.equals(cols[i])?f.length():null;result.addRow(row);return result;}
 public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException("只读更新文件");}
 public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException("只读更新文件");}
 public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException("只读更新文件");}
}
