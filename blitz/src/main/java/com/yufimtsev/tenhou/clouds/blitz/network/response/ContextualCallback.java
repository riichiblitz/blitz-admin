package com.yufimtsev.tenhou.clouds.blitz.network.response;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class ContextualCallback<T> implements Callback<BaseResponse<T>> {

    //public abstract Context getContext();

    public abstract void onSuccess(T data);

    @Override
    public void onResponse(Call<BaseResponse<T>> call, Response<BaseResponse<T>> response) {
        /*if (response.isSuccessful()) {
            if ("ok".equals(response.body().status)) {
                onSuccess(response.body().data);
            } else {
                onFailure(call, new RuntimeException("Status: " + response.body().status));
            }
        } else {
            onFailure(call, new NetworkErrorException("No network"));
        }*/
    }

    @Override
    public void onFailure(final Call<BaseResponse<T>> call, Throwable t) {
        /*new AlertDialog.Builder(getContext())
                .setTitle("Error: " + call.request().method() + " " + call.request().url())
                .setMessage(t.getMessage())
                .setPositiveButton("RETRY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        call.clone().enqueue(ContextualCallback.this);
                    }
                })
                .show();*/
    }
}
